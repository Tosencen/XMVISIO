package com.xmvisio.app.audio

import android.content.Context
import com.xmvisio.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * 音频分类
 */
@Serializable
data class AudioCategory(
    val id: String,
    val name: String
) {
    companion object {
        val ALL = AudioCategory("all", "全部")
    }
}

/**
 * 音频分类映射（音频ID -> 单个分类ID）
 */
@Serializable
data class AudioCategoryMapping(
    val audioId: Long,
    val categoryId: String?  // null 表示未分类
)

/**
 * 分类管理器（SQLite 存储，见 [AppDatabase]）
 */
class CategoryManager(private val context: Context) {

    private val db get() = AppDatabase.getInstance(context)

    /**
     * 获取所有分类
     */
    suspend fun getCategories(): List<AudioCategory> = withContext(Dispatchers.IO) {
        val rows = db.withConnection { conn ->
            conn.prepare("SELECT id, name FROM categories ORDER BY sort_order").use { stmt ->
                buildList {
                    while (stmt.step()) {
                        add(AudioCategory(id = stmt.getText(0), name = stmt.getText(1)))
                    }
                }
            }
        }
        listOf(AudioCategory.ALL) + rows
    }

    /**
     * 添加分类
     */
    suspend fun addCategory(name: String): AudioCategory = withContext(Dispatchers.IO) {
        val newCategory = AudioCategory(
            id = "category_${System.currentTimeMillis()}",
            name = name
        )
        // MAX+1 避免删除分类后 COUNT 与已有 sort_order 冲突
        val sortOrder = db.withConnection { conn ->
            conn.prepare("SELECT COALESCE(MAX(sort_order) + 1, 0) FROM categories").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
        }
        db.inTransaction { conn ->
            conn.prepare("INSERT INTO categories (id, name, sort_order) VALUES (?, ?, ?)").use { stmt ->
                stmt.bindText(1, newCategory.id)
                stmt.bindText(2, newCategory.name)
                stmt.bindLong(3, sortOrder)
                stmt.step()
            }
        }
        newCategory
    }

    /**
     * 删除分类
     */
    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        db.inTransaction { conn ->
            conn.prepare("DELETE FROM categories WHERE id = ?").use { stmt ->
                stmt.bindText(1, categoryId)
                stmt.step()
            }
            // 该分类下的音频变为未分类
            conn.prepare("UPDATE audio_category_mapping SET category_id = NULL WHERE category_id = ?").use { stmt ->
                stmt.bindText(1, categoryId)
                stmt.step()
            }
            // 清理该分类残留的自定义排序
            conn.prepare("DELETE FROM audio_order WHERE category_id = ?").use { stmt ->
                stmt.bindText(1, categoryId)
                stmt.step()
            }
        }
    }

    /**
     * 重命名分类（保留映射关系）
     */
    suspend fun renameCategory(categoryId: String, newName: String) = withContext(Dispatchers.IO) {
        db.withConnection { conn ->
            conn.prepare("UPDATE categories SET name = ? WHERE id = ?").use { stmt ->
                stmt.bindText(1, newName)
                stmt.bindText(2, categoryId)
                stmt.step()
            }
        }
    }

    /**
     * 获取音频的分类ID
     */
    suspend fun getAudioCategory(audioId: Long): String? = withContext(Dispatchers.IO) {
        db.withConnection { conn ->
            // COALESCE 避免读取 NULL 列
            conn.prepare("SELECT COALESCE(category_id, '') FROM audio_category_mapping WHERE audio_id = ?").use { stmt ->
                stmt.bindLong(1, audioId)
                if (stmt.step()) stmt.getText(0).ifEmpty { null } else null
            }
        }
    }

    /**
     * 设置音频的分类（单选）
     */
    suspend fun setAudioCategory(audioId: Long, categoryId: String?) = withContext(Dispatchers.IO) {
        db.withConnection { conn ->
            conn.prepare(
                "INSERT INTO audio_category_mapping (audio_id, category_id) VALUES (?, ?) " +
                    "ON CONFLICT(audio_id) DO UPDATE SET category_id = excluded.category_id"
            ).use { stmt ->
                stmt.bindLong(1, audioId)
                if (categoryId != null) stmt.bindText(2, categoryId) else stmt.bindNull(2)
                stmt.step()
            }
        }
    }

    /**
     * 获取分类下的所有音频ID
     */
    suspend fun getAudioIdsByCategory(categoryId: String): List<Long> = withContext(Dispatchers.IO) {
        if (categoryId == AudioCategory.ALL.id) {
            // "全部"返回空列表，表示不过滤（显示所有音频）
            return@withContext emptyList()
        }
        db.withConnection { conn ->
            conn.prepare("SELECT audio_id FROM audio_category_mapping WHERE category_id = ?").use { stmt ->
                stmt.bindText(1, categoryId)
                buildList {
                    while (stmt.step()) {
                        add(stmt.getLong(0))
                    }
                }
            }
        }
    }

    /**
     * 批量删除音频的分类映射（删除音频文件后清理孤儿数据）
     */
    suspend fun deleteAudioMappings(audioIds: List<Long>) = withContext(Dispatchers.IO) {
        if (audioIds.isEmpty()) return@withContext
        db.inTransaction { conn ->
            conn.prepare("DELETE FROM audio_category_mapping WHERE audio_id = ?").use { stmt ->
                audioIds.forEach { id ->
                    stmt.bindLong(1, id)
                    stmt.step()
                    stmt.reset()
                }
            }
        }
    }

    /**
     * 批量设置音频的分类
     *
     * @param audioIds 音频ID列表
     * @param categoryId 目标分类ID（null表示移除分类）
     * @return 操作结果
     */
    suspend fun setAudioCategoryBatch(
        audioIds: List<Long>,
        categoryId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.inTransaction { conn ->
                conn.prepare(
                    "INSERT INTO audio_category_mapping (audio_id, category_id) VALUES (?, ?) " +
                        "ON CONFLICT(audio_id) DO UPDATE SET category_id = excluded.category_id"
                ).use { stmt ->
                    audioIds.forEach { audioId ->
                        stmt.bindLong(1, audioId)
                        if (categoryId != null) stmt.bindText(2, categoryId) else stmt.bindNull(2)
                        stmt.step()
                        stmt.reset()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CategoryManager", "批量设置分类失败", e)
            Result.failure(e)
        }
    }
}
