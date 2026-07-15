/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
package com.xmvisio.app.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SealDownloadManager private constructor(
    private val context: Context,
    private val onRequestPermission: () -> Unit = {}
) : IDownloadManager {
    
    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    override val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()
    
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun hasStoragePermission(): Boolean { return true }
    override fun requestStoragePermission() { onRequestPermission() }
    override suspend fun updateYtDlp(): YtDlpUpdateStatus = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
            when (status) {
                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE -> YtDlpUpdateStatus.DONE
                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> YtDlpUpdateStatus.ALREADY_UP_TO_DATE
                else -> YtDlpUpdateStatus.ERROR
            }
        } catch (e: Exception) { YtDlpUpdateStatus.ERROR }
    }
    override fun getYtDlpVersion(): String? = try { YoutubeDL.getInstance().version(context) } catch (e: Exception) { null }
    override suspend fun startDownload(url: String, downloadType: DownloadType): Result<String> { /* ... */ return Result.failure(Exception("Disabled")) }
    override fun cancelDownload(taskId: String) {}
    override fun removeDownload(taskId: String) {}
    override suspend fun retryDownload(taskId: String) {}
    override fun clearCompletedDownloads() {}
    override fun clearAllDownloads() {}
    fun close() { downloadScope.cancel() }
    
    companion object {
        @Volatile private var instance: SealDownloadManager? = null
        fun getInstance(context: Context, onRequestPermission: () -> Unit = {}): SealDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: SealDownloadManager(context.applicationContext, onRequestPermission).also { instance = it }
            }
        }
    }
}
*/
