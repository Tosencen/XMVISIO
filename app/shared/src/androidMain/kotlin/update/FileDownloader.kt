package com.xmvisio.app.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 文件下载器（带进度和智能回退）
 */
class FileDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()
    
    /**
     * 下载文件（带智能回退）
     * @param url 主下载URL（jsDelivr CDN）
     * @param destination 目标文件
     * @param fallbackUrl 回退URL（GitHub直接下载）
     * @return 下载成功的文件，失败返回null
     */
    suspend fun download(
        url: String,
        destination: File,
        fallbackUrl: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            _state.value = DownloadState.Downloading
            _progress.value = 0f
            
            Log.d("FileDownloader", "开始下载: $url")
            
            // 尝试主URL下载
            val result = tryDownload(url, destination)
            
            if (result != null) {
                _state.value = DownloadState.Success(result)
                return@withContext result
            }
            
            // 如果主URL失败且有回退URL，尝试回退URL
            if (fallbackUrl != null && fallbackUrl != url) {
                Log.w("FileDownloader", "主URL下载失败，尝试回退URL: $fallbackUrl")
                _progress.value = 0f
                
                val fallbackResult = tryDownload(fallbackUrl, destination)
                
                if (fallbackResult != null) {
                    _state.value = DownloadState.Success(fallbackResult)
                    return@withContext fallbackResult
                }
            }
            
            // 所有尝试都失败
            val error = "下载失败，请检查网络连接"
            _state.value = DownloadState.Failed(error)
            Log.e("FileDownloader", error)
            null
            
        } catch (e: Exception) {
            val error = "下载失败: ${e.message}"
            _state.value = DownloadState.Failed(error)
            Log.e("FileDownloader", error, e)
            null
        }
    }
    
    /**
     * 尝试从指定URL下载文件
     */
    private suspend fun tryDownload(url: String, destination: File): File? {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("FileDownloader", "HTTP错误: ${response.code}")
                return null
            }
            
            val body = response.body ?: run {
                Log.e("FileDownloader", "响应体为空")
                return null
            }
            
            val contentLength = body.contentLength()
            Log.d("FileDownloader", "文件大小: ${contentLength / 1024 / 1024}MB")
            
            // 确保目标目录存在
            destination.parentFile?.mkdirs()
            
            // 下载到临时文件
            val tempFile = File(destination.parentFile, "${destination.name}.tmp")
            
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // 更新进度
                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength
                            _progress.value = progress
                        }
                    }
                }
            }
            
            // 下载完成，重命名临时文件
            if (destination.exists()) {
                destination.delete()
            }
            tempFile.renameTo(destination)
            
            Log.d("FileDownloader", "下载完成: ${destination.absolutePath}")
            destination
            
        } catch (e: IOException) {
            Log.e("FileDownloader", "下载失败: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("FileDownloader", "下载异常: ${e.message}", e)
            null
        }
    }
    
    /**
     * 取消下载
     */
    fun cancel() {
        // OkHttp会在协程取消时自动停止请求
        _state.value = DownloadState.Idle
        _progress.value = 0f
    }
}

/**
 * 下载状态
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
