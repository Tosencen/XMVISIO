package com.xmvisio.app.folder

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.sqlite.execSQL
import com.xmvisio.app.data.AppDatabase
import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.FolderType
import com.xmvisio.app.data.MediaFolder
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.audio.LocalAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 文件夹管理器
 * 负责文件夹的增删改查和持久化存储
 */
class FolderRepository(private val context: Context) {

    // view_mode 等标量偏好仍用 SharedPreferences；列表数据（文件夹/扫描路径）存 SQLite
    private val prefs = context.getSharedPreferences("media_folders", Context.MODE_PRIVATE)
    private val db get() = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folders: StateFlow<List<MediaFolder>> = _folders.asStateFlow()

    private val _scanPaths = MutableStateFlow<List<String>>(emptyList())
    val scanPaths: StateFlow<List<String>> = _scanPaths.asStateFlow()

    private val _viewMode = MutableStateFlow(0)
    val viewMode: StateFlow<Int> = _viewMode.asStateFlow()

    // ContentObserver 用于监听媒体库变化
    private var contentObserver: ContentObserver? = null
    private val _mediaChangeTrigger = MutableStateFlow(0L)
    val mediaChangeTrigger: StateFlow<Long> = _mediaChangeTrigger.asStateFlow()

    companion object {
        private const val KEY_VIEW_MODE = "view_mode"

        @Volatile
        private var instance: FolderRepository? = null

        fun getInstance(context: Context): FolderRepository {
            return instance ?: synchronized(this) {
                instance ?: FolderRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        loadFolders()
        loadScanPaths()
        loadViewMode()
        registerContentObserver()
    }

    // ==================== 文件夹 CRUD ====================

    /**
     * 获取所有文件夹
     */
    fun getAllFolders(): List<MediaFolder> = _folders.value

    /**
     * 获取根级文件夹
     */
    fun getRootFolders(type: FolderType? = null): List<MediaFolder> {
        return _folders.value.filter { folder ->
            folder.parentPath == null && (type == null || folder.type == type || folder.type == FolderType.ALL)
        }.sortedBy { it.sortOrder }
    }

    /**
     * 获取子文件夹
     */
    fun getSubFolders(parentPath: String, type: FolderType? = null): List<MediaFolder> {
        return _folders.value.filter { folder ->
            folder.parentPath == parentPath && (type == null || folder.type == type || folder.type == FolderType.ALL)
        }.sortedBy { it.sortOrder }
    }

    /**
     * 根据路径获取文件夹
     */
    fun getFolderByPath(path: String): MediaFolder? {
        return _folders.value.find { it.path == path }
    }

    /**
     * 获取媒体文件所属的文件夹路径
     */
    fun getFolderPathForMedia(mediaPath: String): String? {
        // 找到最匹配的文件夹（最长路径匹配）
        return _folders.value
            .filter { mediaPath.startsWith(it.path) }
            .maxByOrNull { it.path.length }
            ?.path
    }

    /**
     * 添加文件夹
     */
    fun addFolder(
        name: String,
        path: String,
        type: FolderType = FolderType.ALL,
        parentPath: String? = null
    ): MediaFolder {
        // 检查是否已存在
        val existing = _folders.value.find { it.path == path }
        if (existing != null) return existing

        // 检查是否是其他文件夹的子路径
        val parent = _folders.value.find { path.startsWith(it.path + "/") }
        val actualParentPath = parentPath ?: parent?.path

        val newFolder = MediaFolder(
            id = "folder_${System.currentTimeMillis()}",
            name = name,
            path = path,
            parentPath = actualParentPath,
            type = type,
            sortOrder = _folders.value.size
        )

        val updatedFolders = _folders.value + newFolder
        _folders.value = updatedFolders
        saveFolders(updatedFolders)

        // 创建物理目录
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return newFolder
    }

    /**
     * 重命名文件夹
     */
    fun renameFolder(folderId: String, newName: String): Boolean {
        val index = _folders.value.indexOfFirst { it.id == folderId }
        if (index < 0) return false

        val oldFolder = _folders.value[index]
        val updatedFolder = oldFolder.copy(name = newName)

        val updatedFolders = _folders.value.toMutableList()
        updatedFolders[index] = updatedFolder
        _folders.value = updatedFolders
        saveFolders(updatedFolders)

        // 重命名物理目录
        val oldDir = File(oldFolder.path)
        val newDir = File(oldDir.parent, newName)
        if (oldDir.exists() && !newDir.exists()) {
            oldDir.renameTo(newDir)
            // 更新路径
            updateFolderPath(folderId, newDir.absolutePath)
        }

        return true
    }

    /**
     * 删除文件夹
     */
    fun deleteFolder(folderId: String, deleteFiles: Boolean = false): Boolean {
        val folder = _folders.value.find { it.id == folderId } ?: return false

        // 递归删除子文件夹
        val childFolders = _folders.value.filter { it.parentPath == folder.path }
        childFolders.forEach { deleteFolder(it.id, deleteFiles) }

        // 删除文件夹
        val updatedFolders = _folders.value.filter { it.id != folderId }
        _folders.value = updatedFolders
        saveFolders(updatedFolders)

        // 删除物理目录
        if (deleteFiles) {
            val dir = File(folder.path)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }

        return true
    }

    /**
     * 更新文件夹路径
     */
    private fun updateFolderPath(folderId: String, newPath: String) {
        val index = _folders.value.indexOfFirst { it.id == folderId }
        if (index < 0) return

        val updatedFolders = _folders.value.toMutableList()
        updatedFolders[index] = updatedFolders[index].copy(path = newPath)
        _folders.value = updatedFolders
        saveFolders(updatedFolders)
    }

    // ==================== 扫描路径管理 ====================

    /**
     * 获取自定义扫描路径
     */
    fun getScanPaths(): List<String> = _scanPaths.value

    /**
     * 添加扫描路径
     */
    fun addScanPath(path: String) {
        if (path in _scanPaths.value) return
        val updatedPaths = _scanPaths.value + path
        _scanPaths.value = updatedPaths
        saveScanPaths(updatedPaths)
    }

    /**
     * 移除扫描路径
     */
    fun removeScanPath(path: String) {
        val updatedPaths = _scanPaths.value.filter { it != path }
        _scanPaths.value = updatedPaths
        saveScanPaths(updatedPaths)
    }

    // ==================== 视图模式 ====================

    /**
     * 设置视图模式
     */
    fun setViewMode(mode: Int) {
        _viewMode.value = mode
        prefs.edit().putInt(KEY_VIEW_MODE, mode).apply()
    }

    // ==================== 持久化 ====================

    private fun loadFolders() {
        _folders.value = db.withConnection { conn ->
            conn.prepare(
                "SELECT id, name, path, COALESCE(parent_path, ''), type, sort_order, created_at " +
                    "FROM folders ORDER BY sort_order"
            ).use { stmt ->
                buildList {
                    while (stmt.step()) {
                        val parent = stmt.getText(3)
                        add(
                            MediaFolder(
                                id = stmt.getText(0),
                                name = stmt.getText(1),
                                path = stmt.getText(2),
                                parentPath = parent.ifEmpty { null },
                                type = FolderType.valueOf(stmt.getText(4)),
                                sortOrder = stmt.getLong(5).toInt(),
                                createdAt = stmt.getLong(6)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun saveFolders(folders: List<MediaFolder>) {
        db.inTransaction { conn ->
            conn.execSQL("DELETE FROM folders")
            conn.prepare(
                "INSERT INTO folders (id, name, path, parent_path, type, sort_order, created_at) " +
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
        }
    }

    private fun loadScanPaths() {
        _scanPaths.value = db.withConnection { conn ->
            conn.prepare("SELECT path FROM scan_paths").use { stmt ->
                buildList {
                    while (stmt.step()) {
                        add(stmt.getText(0))
                    }
                }
            }
        }
    }

    private fun saveScanPaths(paths: List<String>) {
        db.inTransaction { conn ->
            conn.execSQL("DELETE FROM scan_paths")
            conn.prepare("INSERT INTO scan_paths (path) VALUES (?)").use { stmt ->
                paths.forEach { p ->
                    stmt.bindText(1, p)
                    stmt.step()
                    stmt.reset()
                }
            }
        }
    }

    private fun loadViewMode() {
        _viewMode.value = prefs.getInt(KEY_VIEW_MODE, 0)
    }

    // ==================== ContentObserver ====================

    private fun registerContentObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                _mediaChangeTrigger.value = System.currentTimeMillis()
            }
        }

        val contentResolver = context.contentResolver
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }

    fun unregisterContentObserver() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
    }

    /**
     * 构建 Folder UI 模型
     */
    fun buildFolder(
        path: String,
        videos: List<VideoInfo> = emptyList(),
        audios: List<LocalAudioFile> = emptyList(),
        subfolders: List<String> = emptyList()
    ): Folder {
        val dir = File(path)
        val directVideos = videos.filter { File(it.path).parent == path }
        val directAudios = audios.filter { it.uri.path?.let { p -> File(p).parent } == path }

        return Folder(
            name = dir.name,
            path = path,
            dateModified = dir.lastModified(),
            parentPath = dir.parentFile?.absolutePath,
            totalSize = directVideos.sumOf { it.size },
            totalDuration = directVideos.sumOf { it.duration } + directAudios.sumOf { it.duration },
            mediaCount = directVideos.size + directAudios.size,
            foldersCount = subfolders.size
        )
    }
}
