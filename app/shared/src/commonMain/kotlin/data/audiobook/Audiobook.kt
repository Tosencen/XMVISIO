package com.xmvisio.app.data.audiobook

/**
 * 有声书 ID
 */
@JvmInline
value class AudiobookId(val value: String)

/**
 * 有声书实体
 */
data class Audiobook(
    val id: AudiobookId,
    val name: String,
    val author: String?,
    val coverPath: String?,
    val currentPosition: Long,
    val totalDuration: Long,
    val addedAt: Long,
    val lastPlayedAt: Long?
) {
    /**
     * 计算播放进度 (0.0 - 1.0)
     */
    fun progress(): Float {
        if (totalDuration == 0L) return 0f
        val progress = currentPosition.toFloat() / totalDuration.toFloat()
        return progress.coerceIn(0f, 1f)
    }
    
    /**
     * 计算剩余时间（秒）
     */
    fun remainingTimeSeconds(): Long {
        return ((totalDuration - currentPosition) / 1000).coerceAtLeast(0)
    }
    
    /**
     * 判断是否已完成
     */
    fun isFinished(): Boolean {
        return progress() >= 0.99f
    }
}

/**
 * 书籍分类
 */
enum class BookCategory {
    CURRENT,    // 当前阅读
    FINISHED    // 已完成
}
