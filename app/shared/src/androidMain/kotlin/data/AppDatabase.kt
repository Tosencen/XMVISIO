package com.xmvisio.app.data

import android.content.Context
import android.util.Log
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.xmvisio.app.audio.AudioCategory
import com.xmvisio.app.audio.AudioCategoryMapping
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 应用数据库（SQLite，sqlite-bundled 驱动）
 *
 * 取代原先用 SharedPreferences 存 JSON 列表的做法：
 * - 真正的表结构，读写原子、可查询
 * - 用 `PRAGMA user_version` 提供 schema 版本迁移能力
 * - 首次升级时自动从旧 SharedPreferences 导入历史数据（迁移成功后清理旧 key）
 *
 * 注意：androidx.sqlite 的 [SQLiteConnection] 非线程安全，
 * 所有访问必须经过 [withConnection]（内部同步）。
 */
class AppDatabase private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val connection: SQLiteConnection

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "xmvisio.db"
        private const val DB_VERSION = 1

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context).also { instance = it }
            }
        }
    }

    init {
        val dbFile = File(appContext.filesDir, DB_NAME)
        connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        migrate()
    }

    /**
     * 线程安全地访问连接
     */
    fun <T> withConnection(block: (SQLiteConnection) -> T): T = synchronized(connection) {
        block(connection)
    }

    /**
     * 在事务中执行（手动 BEGIN/COMMIT/ROLLBACK，失败回滚并重新抛出）。
     * 注意：不可嵌套调用（SQLite 不支持事务内再开事务）。
     */
    fun <T> inTransaction(block: (SQLiteConnection) -> T): T = withConnection { conn ->
        conn.execSQL("BEGIN IMMEDIATE")
        try {
            val result = block(conn)
            conn.execSQL("COMMIT")
            result
        } catch (e: Exception) {
            try { conn.execSQL("ROLLBACK") } catch (_: Exception) {}
            throw e
        }
    }

    // ==================== Schema & 迁移 ====================

    /**
     * 迁移在「单个事务」内完成：DDL + 数据导入 + user_version 一起成功或一起回滚，
     * 避免进程中途被杀导致半迁移状态（下次启动 user_version 仍为 0 会重新导入，
     * 配合 INSERT OR IGNORE 幂等，重复导入不会报错）。
     * 旧 prefs 的清理放在提交成功之后，保证失败时可完整重试。
     */
    private fun migrate() {
        withConnection { conn ->
            val currentVersion = conn.prepare("PRAGMA user_version").use { stmt ->
                if (stmt.step()) stmt.getLong(0).toInt() else 0
            }
            if (currentVersion >= DB_VERSION) return@withConnection

            conn.execSQL("BEGIN IMMEDIATE")
            try {
                createTables(conn)
                if (currentVersion < 1) {
                    importLegacyPrefs(conn)
                }
                conn.execSQL("PRAGMA user_version = $DB_VERSION")
                conn.execSQL("COMMIT")
            } catch (e: Exception) {
                try { conn.execSQL("ROLLBACK") } catch (_: Exception) {}
                throw e
            }
            Log.i(TAG, "数据库迁移完成: v$currentVersion -> v$DB_VERSION")

            // 提交成功后再清理旧 prefs（失败则保留，下次可重试）
            clearLegacyPrefs()
        }
    }

    private fun createTables(conn: SQLiteConnection) {
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS categories (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "sort_order INTEGER NOT NULL DEFAULT 0)"
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS audio_category_mapping (" +
                "audio_id INTEGER PRIMARY KEY, " +
                "category_id TEXT)"
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS audio_order (" +
                "category_id TEXT PRIMARY KEY, " +
                "audio_ids TEXT NOT NULL)"
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS folders (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "path TEXT NOT NULL UNIQUE, " +
                "parent_path TEXT, " +
                "type TEXT NOT NULL DEFAULT 'ALL', " +
                "sort_order INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL DEFAULT 0)"
        )
        conn.execSQL(
            "CREATE TABLE IF NOT EXISTS scan_paths (" +
                "path TEXT PRIMARY KEY)"
        )
    }

    // ==================== 旧数据导入（一次性，幂等）====================

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 只做导入，不开启新事务（由 migrate 的外层事务包裹）。
     * 使用 INSERT OR IGNORE 保证重复导入安全。
     */
    private fun importLegacyPrefs(conn: SQLiteConnection) {
        var importedCategories = 0
        var importedMappings = 0
        var importedOrders = 0
        var importedFolders = 0
        var importedScanPaths = 0

        // 1. 音频分类 + 映射（旧 prefs: audio_categories）
        try {
            val audioPrefs = appContext.getSharedPreferences("audio_categories", Context.MODE_PRIVATE)
            val categoriesJson = audioPrefs.getString("categories", null)
            if (categoriesJson != null) {
                val categories = json.decodeFromString<List<AudioCategory>>(categoriesJson)
                conn.prepare(
                    "INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (?, ?, ?)"
                ).use { stmt ->
                    categories.forEachIndexed { index, cat ->
                        stmt.bindText(1, cat.id)
                        stmt.bindText(2, cat.name)
                        stmt.bindLong(3, index.toLong())
                        stmt.step()
                        stmt.reset()
                    }
                }
                importedCategories = categories.size
            }

            val mappingsJson = audioPrefs.getString("mappings", null)
            if (mappingsJson != null) {
                val mappings = json.decodeFromString<List<AudioCategoryMapping>>(mappingsJson)
                conn.prepare(
                    "INSERT OR IGNORE INTO audio_category_mapping (audio_id, category_id) VALUES (?, ?)"
                ).use { stmt ->
                    mappings.forEach { m ->
                        stmt.bindLong(1, m.audioId)
                        if (m.categoryId != null) stmt.bindText(2, m.categoryId) else stmt.bindNull(2)
                        stmt.step()
                        stmt.reset()
                    }
                }
                importedMappings = mappings.size
            }
        } catch (e: Exception) {
            Log.w(TAG, "导入旧分类数据失败", e)
        }

        // 2. 自定义排序（旧 prefs: audio_order，key 形如 order_xxx）
        try {
            val orderPrefs = appContext.getSharedPreferences("audio_order", Context.MODE_PRIVATE)
            orderPrefs.all.forEach { (key, value) ->
                if (key.startsWith("order_") && value is String) {
                    val categoryId = key.removePrefix("order_")
                    conn.prepare(
                        "INSERT OR IGNORE INTO audio_order (category_id, audio_ids) VALUES (?, ?)"
                    ).use { stmt ->
                        stmt.bindText(1, categoryId)
                        stmt.bindText(2, value)
                        stmt.step()
                    }
                    importedOrders++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "导入旧排序数据失败", e)
        }

        // 3. 文件夹 + 扫描路径（旧 prefs: media_folders）
        try {
            val folderPrefs = appContext.getSharedPreferences("media_folders", Context.MODE_PRIVATE)
            val foldersJson = folderPrefs.getString("folders", null)
            if (foldersJson != null) {
                val folders = json.decodeFromString<List<MediaFolder>>(foldersJson)
                conn.prepare(
                    "INSERT OR IGNORE INTO folders (id, name, path, parent_path, type, sort_order, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)"
                ).use { stmt ->
                    folders.forEach { f ->
                        stmt.bindText(1, f.id)
                        stmt.bindText(2, f.name)
                        stmt.bindText(3, f.path)
                        if (f.parentPath != null) stmt.bindText(4, f.parentPath) else stmt.bindNull(4)
                        stmt.bindText(5, f.type.name)
                        stmt.bindLong(6, f.sortOrder.toLong())
                        stmt.bindLong(7, f.createdAt)
                        stmt.step()
                        stmt.reset()
                    }
                }
                importedFolders = folders.size
            }

            val scanPathsJson = folderPrefs.getString("scan_paths", null)
            if (scanPathsJson != null) {
                val paths = json.decodeFromString<List<String>>(scanPathsJson)
                conn.prepare("INSERT OR IGNORE INTO scan_paths (path) VALUES (?)").use { stmt ->
                    paths.forEach { p ->
                        stmt.bindText(1, p)
                        stmt.step()
                        stmt.reset()
                    }
                }
                importedScanPaths = paths.size
            }
        } catch (e: Exception) {
            Log.w(TAG, "导入旧文件夹数据失败", e)
        }

        Log.i(
            TAG,
            "旧数据导入完成: categories=$importedCategories, mappings=$importedMappings, " +
                "orders=$importedOrders, folders=$importedFolders, scanPaths=$importedScanPaths"
        )
    }

    /**
     * 清理旧 SharedPreferences 列表 key（仅在迁移事务提交成功后调用）
     */
    private fun clearLegacyPrefs() {
        try {
            val audioPrefs = appContext.getSharedPreferences("audio_categories", Context.MODE_PRIVATE)
            audioPrefs.edit().remove("categories").remove("mappings").apply()
        } catch (_: Exception) {}

        try {
            val orderPrefs = appContext.getSharedPreferences("audio_order", Context.MODE_PRIVATE)
            orderPrefs.edit().clear().apply()
        } catch (_: Exception) {}

        try {
            val folderPrefs = appContext.getSharedPreferences("media_folders", Context.MODE_PRIVATE)
            folderPrefs.edit().remove("folders").remove("scan_paths").apply()
        } catch (_: Exception) {}
    }
}
