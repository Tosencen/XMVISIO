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
    val downloadType: DownloadType = DownloadType.AUDIO,  // 下载类型
    val duration: Long = 0,  // 时长（秒）
    val fileSize: Long = 0   // 文件大小（字节）
)

enum class DownloadStatus {
    PENDING,
    EXTRACTING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DownloadType {
    AUDIO,  // 仅音频
    VIDEO   // 视频（包含音频）
}
