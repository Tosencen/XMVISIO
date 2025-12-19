package com.xmvisio.app.download

import kotlinx.coroutines.flow.StateFlow

interface IDownloadManager {
    val downloads: StateFlow<List<DownloadTask>>
    
    suspend fun startDownload(url: String, downloadType: DownloadType = DownloadType.AUDIO): Result<String>
    fun cancelDownload(taskId: String)
    fun removeDownload(taskId: String)  // 移除单个下载记录
    fun hasStoragePermission(): Boolean
    fun requestStoragePermission()
    
    // 清理下载记录
    fun clearCompletedDownloads()  // 清理已完成的下载记录
    fun clearAllDownloads()  // 清理所有下载记录（不包括正在下载的）
    
    // yt-dlp 更新相关
    suspend fun updateYtDlp(): YtDlpUpdateStatus
    fun getYtDlpVersion(): String?
}

enum class YtDlpUpdateStatus {
    IDLE,
    UPDATING,
    DONE,
    ALREADY_UP_TO_DATE,
    ERROR
}
