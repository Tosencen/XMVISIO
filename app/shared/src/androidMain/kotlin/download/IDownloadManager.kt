package com.xmvisio.app.download

import kotlinx.coroutines.flow.StateFlow

interface IDownloadManager {
    val downloads: StateFlow<List<DownloadTask>>
    
    suspend fun startDownload(url: String, downloadType: DownloadType = DownloadType.AUDIO): Result<String>
    fun cancelDownload(taskId: String)
    fun hasStoragePermission(): Boolean
    fun requestStoragePermission()
    
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
