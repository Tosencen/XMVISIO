package com.xmvisio.app.ui.main

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * VideoScreen 的状态持有者（ViewModel）
 * 统一管理所有 UI 状态和业务逻辑，让 Composable 只负责渲染。
 *
 * 作为 ViewModel 与组合解耦：从设置页返回、切换 Tab 时状态保留，
 * 避免重复全量查询 MediaStore。
 */
class VideoScreenState(context: Context) : ViewModel() {
    /** 只持有 ApplicationContext，避免持有 Activity 造成内存泄漏 */
    private val context: Context = context.applicationContext
    /**
     * Intent 启动器回调（由 composable 在创建后绑定）
     */
    var onLaunchIntent: (IntentSenderRequest) -> Unit = {}
    // ===== 权限 =====
    val videoPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by mutableStateOf(
        ContextCompat.checkSelfPermission(context, videoPermission) ==
                PackageManager.PERMISSION_GRANTED
    )

    // ===== 视频数据 =====
    var videos by mutableStateOf<List<VideoInfo>>(emptyList())
    var isLoading by mutableStateOf(true)
    var isRefreshing by mutableStateOf(false)
    var refreshTrigger by mutableIntStateOf(0)

    // ContentObserver 防抖
    var lastObserverChange by mutableLongStateOf(0L)

    // ===== 布局设置 =====
    var sortBy by mutableIntStateOf(1)
    var sortAscending by mutableStateOf(false)
    var isGridLayout by mutableStateOf(true)
    var viewMode by mutableIntStateOf(0) // 0=树形, 1=文件夹, 2=全部

    // ===== 导航 =====
    var currentFolderPath by mutableStateOf<String?>(null)

    // ===== 视频操作待处理状态（用于 MediaStore 请求回调）=====
    var pendingVideoUri by mutableStateOf<String?>(null)
    var pendingRenameName by mutableStateOf<String?>(null)
    var pendingOp by mutableStateOf("") // "delete" / "rename"
    var pendingFolderOp by mutableStateOf("") // "rename" / "delete"
    var pendingFolderUris by mutableStateOf<List<String>>(emptyList())
    var pendingFolderOldPath by mutableStateOf<String?>(null)
    var pendingFolderNewPath by mutableStateOf<String?>(null)

    // ===== 对话框/菜单可见性 =====
    var showQuickSettings by mutableStateOf(false)
    var showVideoMenu by mutableStateOf(false)
    var showRenameDialog by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)
    var showPropertiesDialog by mutableStateOf(false)
    var showFolderMenu by mutableStateOf(false)
    var showFolderRenameDialog by mutableStateOf(false)
    var showFolderDeleteDialog by mutableStateOf(false)

    // ===== 选中项 =====
    var selectedVideo by mutableStateOf<VideoInfo?>(null)
    var selectedFolder by mutableStateOf<Folder?>(null)
    var renameText by mutableStateOf("")
    var folderRenameText by mutableStateOf("")

    // ===== 持久化（SharedPreferences）=====
    private val prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)

    fun loadPersistedSettings() {
        viewModelScope.launch {
            isGridLayout = prefs.getBoolean("grid_layout", true)
            sortBy = prefs.getInt("sort_by", 1)
            sortAscending = prefs.getBoolean("sort_asc", false)
            viewMode = prefs.getInt("view_mode", 0)
        }
    }

    fun persistSettings() {
        prefs.edit()
            .putBoolean("grid_layout", isGridLayout)
            .putInt("sort_by", sortBy)
            .putBoolean("sort_asc", sortAscending)
            .putInt("view_mode", viewMode)
            .apply()
    }

    // ===== 计算属性：文件夹列表（逻辑见 VideoListLogic.kt，纯函数可单测）=====
    val allFolders: List<Folder> by derivedStateOf {
        if (viewMode == 2) emptyList() else computeFolderHierarchy(videos)
    }

    private val folderPathSet: Set<String> by derivedStateOf {
        allFolders.map { it.path }.toSet()
    }

    val displayFolders: List<Folder> by derivedStateOf {
        when (viewMode) {
            0, 1 -> computeFolderTree(allFolders, currentFolderPath)
            else -> emptyList()
        }
    }

    val sortedVideos: List<VideoInfo> by derivedStateOf {
        computeSortedVideos(videos, sortBy, sortAscending)
    }

    val currentFolderVideos: List<VideoInfo> by derivedStateOf {
        computeFolderVideos(sortedVideos, viewMode, currentFolderPath, folderPathSet)
    }

    val breadcrumbPath: List<String> by derivedStateOf {
        computeBreadcrumbPath(allFolders, currentFolderPath)
    }

    // ===== 操作：创建 ContentObserver =====
    fun createContentObserver(): ContentObserver {
        return object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()
                if (now - lastObserverChange > 300) {
                    lastObserverChange = now
                    refreshTrigger++
                }
            }
        }
    }

    // ===== 操作：加载视频 =====
    fun loadVideos() {
        viewModelScope.launch {
            if (hasPermission) {
                if (refreshTrigger > 0) isRefreshing = true
                delay(300)
                withContext(Dispatchers.IO) {
                    videos = queryVideos(context)
                    isLoading = false
                    isRefreshing = false
                }
            } else {
                videos = emptyList()
                isLoading = false
                isRefreshing = false
            }
        }
    }

    // ===== 操作：刷新 =====
    fun refresh() {
        isRefreshing = true
        refreshTrigger++
    }

    // ===== 操作：导航 =====
    fun navigateToParentFolder() {
        val parentPath = allFolders.find { it.path == currentFolderPath }?.parentPath
        currentFolderPath = if (parentPath != null && allFolders.any { it.path == parentPath }) parentPath else null
    }

    // ===== 操作：显示 Toast =====
    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ===== 操作：清理待处理状态 =====
    fun clearPendingOps() {
        pendingVideoUri = null
        pendingRenameName = null
        pendingOp = ""
        pendingFolderOp = ""
        pendingFolderUris = emptyList()
        pendingFolderOldPath = null
        pendingFolderNewPath = null
    }

    // ===== 操作：处理 MediaStore 请求结果 =====
    fun handleMediaRequestResult(resultCode: Int) {
        if (resultCode == android.app.Activity.RESULT_OK && (pendingVideoUri != null || pendingFolderOp.isNotEmpty())) {
            viewModelScope.launch {
                when {
                    pendingFolderOp == "delete" -> {
                        withContext(Dispatchers.IO) {
                            pendingFolderOldPath?.let { File(it).deleteRecursively() }
                        }
                        showToast("已删除文件夹")
                    }
                    pendingFolderOp == "rename" -> {
                        handleFolderRenameResult()
                    }
                    pendingVideoUri != null -> {
                        handleVideoOpResult()
                    }
                }
                refreshTrigger++
                clearPendingOps()
            }
        } else {
            when (pendingOp) {
                "delete" -> showToast("已取消删除")
                "rename" -> showToast("已取消重命名")
            }
            when (pendingFolderOp) {
                "delete" -> showToast("已取消删除")
                "rename" -> showToast("已取消重命名")
            }
            clearPendingOps()
        }
    }

    private suspend fun handleFolderRenameResult() {
        val oldPath = pendingFolderOldPath
        val newPath = pendingFolderNewPath
        if (oldPath != null && newPath != null) {
            val storageRoot = android.os.Environment.getExternalStorageDirectory().path + "/"
            val oldRelFolder = oldPath.removePrefix(storageRoot).trimEnd('/')
            val newRelFolder = newPath.removePrefix(storageRoot).trimEnd('/')
            val ok = withContext(Dispatchers.IO) {
                try {
                    pendingFolderUris.forEach { uriStr ->
                        val uri = Uri.parse(uriStr)
                        val curRel = queryRelativePath(context, uri).trimEnd('/')
                        val curName = queryDisplayName(context, uri)
                        val suffix = if (curRel == oldRelFolder) "" else curRel.removePrefix("$oldRelFolder/")
                        val newRel = if (suffix.isEmpty()) newRelFolder else "$newRelFolder/$suffix"
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "$newRel/")
                            put(MediaStore.Video.Media.DISPLAY_NAME, curName)
                        }
                        context.contentResolver.update(uri, values, null, null)
                    }
                    File(oldPath).deleteRecursively()
                    true
                } catch (_: Exception) { false }
            }
            showToast(if (ok) "已重命名" else "重命名失败")
        } else {
            showToast("重命名失败")
        }
    }

    private suspend fun handleVideoOpResult() {
        val uri = Uri.parse(pendingVideoUri!!)
        if (pendingRenameName != null) {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, pendingRenameName)
                    }
                    context.contentResolver.update(uri, values, null, null) > 0
                } catch (_: Exception) { false }
            }
            showToast(if (ok) "已重命名" else "重命名失败")
        } else {
            showToast("已删除")
        }
    }

    // ===== 操作：单视频重命名 =====
    fun renameVideo(video: VideoInfo, newName: String) {
        showRenameDialog = false
        val extension = video.name.substringAfterLast(".", "")
        val finalName = if (extension.isNotEmpty()) "$newName.$extension" else newName
        val uri = Uri.parse(video.uri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                pendingVideoUri = video.uri
                pendingRenameName = finalName
                pendingOp = "rename"
                onLaunchIntent(IntentSenderRequest.Builder(intent).build())
            } catch (_: Exception) { showToast("重命名失败") }
        } else {
            viewModelScope.launch {
                val ok = try {
                    withContext(Dispatchers.IO) {
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, finalName)
                        }
                        context.contentResolver.update(uri, values, null, null) > 0
                    }
                } catch (_: Exception) { false }
                showToast(if (ok) "已重命名" else "重命名失败")
                if (ok) refreshTrigger++
            }
        }
    }

    // ===== 操作：单视频删除 =====
    fun deleteVideo(video: VideoInfo) {
        showDeleteDialog = false
        val uri = Uri.parse(video.uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                pendingVideoUri = video.uri
                pendingRenameName = null
                pendingOp = "delete"
                onLaunchIntent(IntentSenderRequest.Builder(intent).build())
            } catch (_: Exception) { showToast("删除失败") }
        } else {
            viewModelScope.launch {
                val ok = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.delete(uri, null, null) > 0
                    }
                } catch (_: Exception) { false }
                showToast(if (ok) "已删除" else "删除失败")
                if (ok) refreshTrigger++
            }
        }
    }

    // ===== 操作：文件夹重命名 =====
    fun renameFolder(folder: Folder, newName: String) {
        showFolderRenameDialog = false
        if (newName.isEmpty() || newName == folder.name) return

        val parentDir = File(folder.path).parent
        val newPath = if (parentDir != null) "$parentDir/$newName" else newName

        if (File(newPath).exists()) {
            showToast("目标目录已存在")
            return
        }

        // Android 11+ 限制：视频只能放在 DCIM/Movies/Pictures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageRoot = android.os.Environment.getExternalStorageDirectory().path + "/"
            val newTopLevel = File(newPath).path
                .removePrefix(storageRoot).split("/").first()
            val allowedVideoTopLevels = setOf("DCIM", "Movies", "Pictures")
            if (newTopLevel !in allowedVideoTopLevels) {
                showToast("视频只能存放在 DCIM、Movies、Pictures 目录及其子目录中")
                return
            }
        }

        val folderTreeVideos = videos.filter { it.path.startsWith(folder.path + "/") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = folderTreeVideos.map { Uri.parse(it.uri) }
            if (uris.isNotEmpty()) {
                try {
                    val intent = MediaStore.createWriteRequest(context.contentResolver, uris)
                    pendingFolderOp = "rename"
                    pendingFolderUris = folderTreeVideos.map { it.uri }
                    pendingFolderOldPath = folder.path
                    pendingFolderNewPath = newPath
                    onLaunchIntent(IntentSenderRequest.Builder(intent).build())
                } catch (_: Exception) { showToast("重命名失败") }
            } else {
                viewModelScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try { File(folder.path).renameTo(File(newPath)) } catch (_: Exception) { false }
                    }
                    showToast(if (ok) "已重命名" else "重命名失败")
                    if (ok) refreshTrigger++
                }
            }
        } else {
            viewModelScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val moved = File(folder.path).renameTo(File(newPath))
                        if (moved) {
                            folderTreeVideos.forEach { video ->
                                val values = ContentValues().apply {
                                    put(MediaStore.Video.Media.DATA, video.path.replace(folder.path, newPath))
                                }
                                context.contentResolver.update(Uri.parse(video.uri), values, null, null)
                            }
                        }
                        moved
                    } catch (_: Exception) { false }
                }
                showToast(if (ok) "已重命名" else "重命名失败")
                if (ok) refreshTrigger++
            }
        }
    }

    // ===== 操作：文件夹删除 =====
    fun deleteFolder(folder: Folder) {
        showFolderDeleteDialog = false
        val folderTreeVideos = videos.filter { it.path.startsWith(folder.path + "/") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = folderTreeVideos.map { Uri.parse(it.uri) }
            if (uris.isNotEmpty()) {
                try {
                    val intent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    pendingFolderOp = "delete"
                    pendingFolderUris = folderTreeVideos.map { it.uri }
                    pendingFolderOldPath = folder.path
                    pendingFolderNewPath = null
                    onLaunchIntent(IntentSenderRequest.Builder(intent).build())
                } catch (_: Exception) { showToast("删除失败") }
            } else {
                viewModelScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try { File(folder.path).deleteRecursively(); true } catch (_: Exception) { false }
                    }
                    showToast(if (ok) "已删除文件夹" else "删除失败")
                    if (ok) refreshTrigger++
                }
            }
        } else {
            viewModelScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        var allOk = true
                        folderTreeVideos.forEach { video ->
                            val r = context.contentResolver.delete(Uri.parse(video.uri), null, null)
                            if (r <= 0) allOk = false
                        }
                        File(folder.path).deleteRecursively()
                        allOk
                    } catch (_: Exception) { false }
                }
                showToast(if (ok) "已删除文件夹" else "删除失败")
                if (ok) refreshTrigger++
            }
        }
    }

    // ===== 操作：向上导航（BackHandler） =====
    fun onBackPressed(): Boolean {
        if (currentFolderPath != null) {
            val parentPath = allFolders.find { it.path == currentFolderPath }?.parentPath
            currentFolderPath = if (parentPath != null && allFolders.any { it.path == parentPath }) parentPath else null
            return true
        }
        return false
    }
}

// ===== 顶层工具函数 =====

/**
 * 查询视频列表（从 MediaStore）
 */
internal suspend fun queryVideos(context: Context): List<VideoInfo> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<VideoInfo>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.DATA,
    )
    val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        collection, projection, null, null, sortOrder
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val name = cursor.getString(nameIndex) ?: continue
            val duration = cursor.getLong(durationIndex)
            val size = cursor.getLong(sizeIndex)
            val dateModified = cursor.getLong(dateIndex)
            val path = cursor.getString(pathIndex) ?: ""
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
            ).toString()

            videos.add(VideoInfo(id = id, uri = uri, name = name, duration = duration, size = size, dateModified = dateModified, path = path))
        }
    }
    videos
}

/**
 * 加载视频缩略图
 */
internal suspend fun loadVideoThumbnail(context: Context, videoId: Long): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId)
            context.contentResolver.loadThumbnail(uri, android.util.Size(512, 384), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver, videoId,
                MediaStore.Video.Thumbnails.MINI_KIND, null
            )
        }
    } catch (_: Exception) { null }
}

/**
 * 格式化文件大小
 */
internal fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

/**
 * 查询视频的 RELATIVE_PATH
 */
internal fun queryRelativePath(context: Context, uri: Uri): String = try {
    val projection = arrayOf(MediaStore.Video.Media.RELATIVE_PATH)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) ?: "" else ""
    } ?: ""
} catch (_: Exception) { "" }

/**
 * 查询视频的 DISPLAY_NAME
 */
internal fun queryDisplayName(context: Context, uri: Uri): String = try {
    val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) ?: "" else ""
    } ?: ""
} catch (_: Exception) { "" }