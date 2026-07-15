/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
package com.xmvisio.app.download

import kotlinx.coroutines.flow.StateFlow

interface IDownloadManager {
    val downloads: StateFlow<List<DownloadTask>>
    suspend fun startDownload(url: String, downloadType: DownloadType = DownloadType.AUDIO): Result<String>
    fun cancelDownload(taskId: String)
    fun removeDownload(taskId: String)
    suspend fun retryDownload(taskId: String)
    fun hasStoragePermission(): Boolean
    fun requestStoragePermission()
    fun clearCompletedDownloads()
    fun clearAllDownloads()
    suspend fun updateYtDlp(): YtDlpUpdateStatus
    fun getYtDlpVersion(): String?
}

enum class YtDlpUpdateStatus {
    IDLE, UPDATING, DONE, ALREADY_UP_TO_DATE, ERROR
}
*/
