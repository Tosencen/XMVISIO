package com.xmvisio.app.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 基于 Seal 的简化下载管理器
 * 参考: https://github.com/JunkFood02/Seal
 */
class SealDownloadManager private constructor(
    private val context: Context,
    private val onRequestPermission: () -> Unit = {}
) : IDownloadManager {
    
    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    override val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()
    
    // 用于管理下载任务的协程作用域
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun hasStoragePermission(): Boolean {
        // 所有 Android 版本都使用应用外部存储，不需要权限
        // 文件保存在: /storage/emulated/0/Android/data/com.xmvisio.app/files/Music(或Movies)/XMVISIO
        // 用户可以通过文件管理器访问
        return true
    }
    
    override fun requestStoragePermission() {
        onRequestPermission()
    }
    
    override suspend fun updateYtDlp(): YtDlpUpdateStatus = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting yt-dlp update...")
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
            Log.d(TAG, "Update status: $status")
            
            when (status) {
                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE -> {
                    Log.d(TAG, "yt-dlp updated successfully")
                    YtDlpUpdateStatus.DONE
                }
                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> {
                    Log.d(TAG, "yt-dlp already up to date")
                    YtDlpUpdateStatus.ALREADY_UP_TO_DATE
                }
                else -> {
                    Log.w(TAG, "Unknown update status: $status")
                    YtDlpUpdateStatus.ERROR
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update yt-dlp", e)
            YtDlpUpdateStatus.ERROR
        }
    }
    
    override fun getYtDlpVersion(): String? {
        return try {
            YoutubeDL.getInstance().version(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get yt-dlp version", e)
            null
        }
    }
    
    override suspend fun startDownload(url: String, downloadType: DownloadType): Result<String> {
        val taskId = UUID.randomUUID().toString()
        
        // 创建初始任务
        val task = DownloadTask(
            id = taskId,
            url = url,
            title = "正在解析...",
            status = DownloadStatus.EXTRACTING,
            downloadType = downloadType
        )
        addTask(task)
        
        // 在后台协程中执行下载，不阻塞调用者
        downloadScope.launch {
            try {
                // 检查 YoutubeDL 是否已初始化
                try {
                    YoutubeDL.getInstance()
                } catch (e: Exception) {
                    Log.e(TAG, "YoutubeDL not initialized, attempting to initialize now", e)
                    try {
                        YoutubeDL.getInstance().init(context)
                        Log.d(TAG, "YoutubeDL initialized successfully")
                    } catch (initError: Exception) {
                        Log.e(TAG, "Failed to initialize YoutubeDL", initError)
                        updateTask(taskId) {
                            it.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = "YoutubeDL 初始化失败: ${initError.message}"
                            )
                        }
                        return@launch
                    }
                }
                
                // 执行下载任务
                performDownload(taskId, url, downloadType)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message ?: "下载失败"
                    )
                }
            }
        }
        
        // 立即返回任务ID
        return Result.success(taskId)
    }
    
    /**
     * 执行实际的下载任务
     */
    private suspend fun performDownload(taskId: String, url: String, downloadType: DownloadType) = withContext(Dispatchers.IO) {
        try {
            
            // 获取视频信息
            val infoRequest = YoutubeDLRequest(url)
            infoRequest.addOption("--dump-json")
            infoRequest.addOption("--no-playlist")
            infoRequest.addOption("--extractor-args", "youtube:player_client=android,web")
            infoRequest.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            infoRequest.addOption("--socket-timeout", "30")
            infoRequest.addOption("--retries", "10")
            
            val response = YoutubeDL.getInstance().execute(infoRequest)
            val videoInfo = response.out
            
            // 解析视频信息
            val title = extractTitle(videoInfo) ?: "未知标题"
            val duration = extractDuration(videoInfo)
            val fileSize = extractFileSize(videoInfo)
            
            updateTask(taskId) {
                it.copy(
                    title = title,
                    status = DownloadStatus.DOWNLOADING,
                    duration = duration,
                    fileSize = fileSize
                )
            }
            
            // 先下载到临时目录
            val tempDir = File(context.cacheDir, "downloads")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            Log.d(TAG, "Temp download directory: ${tempDir.absolutePath}")
            
            val downloadRequest = YoutubeDLRequest(url)
            
            // 添加通用选项来绕过 YouTube 限制
            downloadRequest.addOption("--no-playlist")
            downloadRequest.addOption("--extractor-args", "youtube:player_client=android,web")
            downloadRequest.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            
            // 添加网络配置，防止超时
            downloadRequest.addOption("--socket-timeout", "30")  // Socket 超时 30 秒
            downloadRequest.addOption("--retries", "10")  // 重试 10 次
            downloadRequest.addOption("--fragment-retries", "10")  // 片段重试 10 次
            
            when (downloadType) {
                DownloadType.AUDIO -> {
                    // 下载音频并使用 FFmpeg 转换为 MP3 格式
                    downloadRequest.addOption("-x")  // 提取音频
                    downloadRequest.addOption("--audio-format", "mp3")  // 转换为 MP3
                    downloadRequest.addOption("--audio-quality", "0")  // 最佳音质
                    downloadRequest.addOption("-o", "${tempDir.absolutePath}/%(title)s.%(ext)s")
                }
                DownloadType.VIDEO -> {
                    // 下载视频+音频，使用 FFmpeg 合并为 MP4 格式
                    // bestvideo+bestaudio 确保同时下载视频和音频流
                    downloadRequest.addOption("-f", "bestvideo+bestaudio/best")
                    downloadRequest.addOption("--merge-output-format", "mp4")  // 使用 FFmpeg 合并
                    downloadRequest.addOption("-o", "${tempDir.absolutePath}/%(title)s.%(ext)s")
                }
            }
            
            // 执行下载
            YoutubeDL.getInstance().execute(downloadRequest) { progress, etaInSeconds, line ->
                // 解析下载信息（如果有的话）
                val downloadedBytes = extractDownloadedBytes(line)
                val totalBytes = extractTotalBytes(line)
                
                updateTask(taskId) {
                    it.copy(
                        progress = progress / 100f,
                        downloadedBytes = downloadedBytes ?: it.downloadedBytes,
                        totalBytes = totalBytes ?: it.totalBytes
                    )
                }
            }
            
            // 下载完成，使用 MediaStore API 保存到公共目录
            val downloadedFiles = tempDir.listFiles()?.filter { it.isFile } ?: emptyList()
            if (downloadedFiles.isEmpty()) {
                throw Exception("下载完成但未找到文件")
            }
            
            val downloadedFile = downloadedFiles.first()
            Log.d(TAG, "Downloaded file: ${downloadedFile.absolutePath}")
            
            // 根据文件扩展名确定 MIME 类型和实际文件类型
            val extension = downloadedFile.extension.lowercase()
            val mimeType = when (extension) {
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "m4a" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                "opus" -> "audio/opus"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                else -> if (downloadType == DownloadType.VIDEO) "video/mp4" else "audio/mpeg"
            }
            
            // 根据实际 MIME 类型决定使用哪个 MediaStore 集合
            // webm 虽然可能只包含音频，但仍然是视频容器格式
            val isVideoFile = mimeType.startsWith("video/")
            
            // 使用 MediaStore API 保存到公共目录
            val contentResolver = context.contentResolver
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (isVideoFile) {
                    android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
            } else {
                if (isVideoFile) {
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            }
            
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, downloadedFile.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // 直接保存到 Music 或 Movies 根目录，不创建子目录
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, 
                        if (isVideoFile) Environment.DIRECTORY_MOVIES 
                        else Environment.DIRECTORY_MUSIC)
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            
            val uri = contentResolver.insert(collection, contentValues)
            if (uri == null) {
                throw Exception("无法创建 MediaStore 条目")
            }
            
            // 写入文件内容
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                downloadedFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // 标记文件为完成
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            
            // 触发媒体扫描，让系统立即识别新文件
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                context.sendBroadcast(intent)
                Log.d(TAG, "Media scanner broadcast sent for: $uri")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send media scanner broadcast", e)
            }
            
            downloadedFile.delete()
            
            // 获取文件的实际路径用于显示
            val filePath = try {
                val cursor = contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DATA), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (columnIndex >= 0) it.getString(columnIndex) else null
                    } else null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get file path", e)
                null
            }
            
            Log.d(TAG, "File saved to MediaStore: $uri")
            Log.d(TAG, "File path: $filePath")
            
            updateTask(taskId) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    filePath = filePath ?: uri.toString()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            updateTask(taskId) {
                it.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "下载失败"
                )
            }
        }
    }
    
    override fun cancelDownload(taskId: String) {
        updateTask(taskId) {
            it.copy(status = DownloadStatus.CANCELLED)
        }
    }
    
    override fun removeDownload(taskId: String) {
        _downloads.value = _downloads.value.filter { it.id != taskId }
    }
    
    override fun clearCompletedDownloads() {
        _downloads.value = _downloads.value.filter { task ->
            task.status != DownloadStatus.COMPLETED
        }
    }
    
    override fun clearAllDownloads() {
        _downloads.value = _downloads.value.filter { task ->
            // 保留正在下载、解析中、等待中的任务
            task.status == DownloadStatus.DOWNLOADING ||
            task.status == DownloadStatus.EXTRACTING ||
            task.status == DownloadStatus.PENDING
        }
    }
    
    private fun addTask(task: DownloadTask) {
        _downloads.value = _downloads.value + task
    }
    
    private fun updateTask(taskId: String, update: (DownloadTask) -> DownloadTask) {
        _downloads.value = _downloads.value.map { task ->
            if (task.id == taskId) update(task) else task
        }
    }
    
    private fun extractTitle(json: String): String? {
        return try {
            // 尝试多种方式提取标题
            // 方法1: 使用正则提取 "title": "xxx"
            val titleRegex = "\"title\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val match = titleRegex.find(json)
            if (match != null) {
                val title = match.groupValues[1]
                // 解码 Unicode 转义序列
                return title.replace("\\\\u([0-9a-fA-F]{4})".toRegex()) {
                    it.groupValues[1].toInt(16).toChar().toString()
                }
            }
            
            // 方法2: 如果没找到，尝试从 URL 提取
            val urlRegex = "\"webpage_url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val urlMatch = urlRegex.find(json)
            if (urlMatch != null) {
                val url = urlMatch.groupValues[1]
                // 从 URL 中提取视频 ID 作为备用标题
                val idRegex = "[?&]v=([^&]+)".toRegex()
                val idMatch = idRegex.find(url)
                if (idMatch != null) {
                    return "视频 ${idMatch.groupValues[1]}"
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract title", e)
            null
        }
    }
    
    private fun extractDuration(json: String): Long {
        return try {
            // 提取 "duration": 123 或 "duration": 123.45
            val durationRegex = "\"duration\"\\s*:\\s*([0-9.]+)".toRegex()
            val match = durationRegex.find(json)
            match?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract duration", e)
            0
        }
    }
    
    private fun extractFileSize(json: String): Long {
        return try {
            // 提取 "filesize" 或 "filesize_approx"
            val fileSizeRegex = "\"filesize(?:_approx)?\"\\s*:\\s*([0-9]+)".toRegex()
            val match = fileSizeRegex.find(json)
            match?.groupValues?.get(1)?.toLongOrNull() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract file size", e)
            0
        }
    }
    
    private fun extractDownloadedBytes(line: String): Long? {
        // 尝试从日志行中提取已下载字节数
        return try {
            val regex = "([0-9.]+)([KMG]i?B)".toRegex()
            val match = regex.find(line)
            match?.let {
                val value = it.groupValues[1].toDoubleOrNull() ?: return null
                val unit = it.groupValues[2]
                when {
                    unit.startsWith("K") -> (value * 1024).toLong()
                    unit.startsWith("M") -> (value * 1024 * 1024).toLong()
                    unit.startsWith("G") -> (value * 1024 * 1024 * 1024).toLong()
                    else -> value.toLong()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractTotalBytes(line: String): Long? {
        // 尝试从日志行中提取总字节数
        return try {
            val regex = "of\\s+([0-9.]+)([KMG]i?B)".toRegex()
            val match = regex.find(line)
            match?.let {
                val value = it.groupValues[1].toDoubleOrNull() ?: return null
                val unit = it.groupValues[2]
                when {
                    unit.startsWith("K") -> (value * 1024).toLong()
                    unit.startsWith("M") -> (value * 1024 * 1024).toLong()
                    unit.startsWith("G") -> (value * 1024 * 1024 * 1024).toLong()
                    else -> value.toLong()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        private const val TAG = "SealDownloadManager"
        
        @Volatile
        private var instance: SealDownloadManager? = null
        
        fun getInstance(context: Context, onRequestPermission: () -> Unit = {}): SealDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: SealDownloadManager(context.applicationContext, onRequestPermission).also {
                    instance = it
                }
            }
        }
    }
}
