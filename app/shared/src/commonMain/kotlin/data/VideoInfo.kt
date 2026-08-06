package com.xmvisio.app.data

data class VideoInfo(
    val id: Long,
    val uri: String,
    val name: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long,
    val path: String = "",  // 文件系统路径
    val width: Int = 0,     // 视频宽（像素），未知为 0
    val height: Int = 0,    // 视频高（像素），未知为 0
) {
    /** 分辨率文案，如 "1920x1080"；未知时为空串 */
    val formattedResolution: String
        get() = if (width > 0 && height > 0) "${width}x$height" else ""

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
}
