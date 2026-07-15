/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
package com.xmvisio.app.download

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val thumbnail: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadType: DownloadType = DownloadType.AUDIO,
    val duration: Long = 0,
    val fileSize: Long = 0
)

enum class DownloadStatus {
    PENDING, EXTRACTING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}

enum class DownloadType {
    AUDIO, VIDEO
}
*/
