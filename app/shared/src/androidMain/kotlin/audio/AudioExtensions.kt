package com.xmvisio.app.audio

import android.content.Context
import com.xmvisio.app.ui.audiobook.AudiobookItemViewState
import com.xmvisio.app.data.audiobook.AudiobookId
import com.xmvisio.app.data.audiobook.BookCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 将LocalAudioFile转换为AudiobookItemViewState
 */
suspend fun LocalAudioFile.toItemViewState(
    context: Context,
    positionManager: PlaybackPositionManager
): AudiobookItemViewState = withContext(Dispatchers.IO) {
    val position = positionManager.getPosition(id)
    val progress = calculateProgress(position, duration.milliseconds)
    val remainingTime = formatRemainingTime(duration.milliseconds - position)
    val category = if (progress >= 0.95f) BookCategory.FINISHED else BookCategory.CURRENT
    
    AudiobookItemViewState(
        id = AudiobookId(id.toString()),
        name = title,
        author = artist,
        coverPath = null, // TODO: 从媒体库获取封面
        progress = progress,
        remainingTime = remainingTime,
        category = category
    )
}

/**
 * 计算播放进度（0.0 - 1.0）
 */
private fun calculateProgress(position: Duration, duration: Duration): Float {
    if (duration.inWholeMilliseconds <= 0) return 0f
    val progress = position.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()
    return progress.coerceIn(0f, 1f)
}

/**
 * 格式化剩余时间
 */
private fun formatRemainingTime(remaining: Duration): String {
    val totalSeconds = remaining.inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return when {
        hours > 0 -> "${hours}小时${minutes}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> "不到1分钟"
    }
}
