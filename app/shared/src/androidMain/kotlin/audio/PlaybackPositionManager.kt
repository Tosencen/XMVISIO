package com.xmvisio.app.audio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 播放位置管理器
 * 负责保存和恢复音频播放位置
 */
class PlaybackPositionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "playback_positions",
        Context.MODE_PRIVATE
    )
    
    /**
     * 保存播放位置
     */
    suspend fun savePosition(audioId: Long, position: Duration) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putLong("position_$audioId", position.inWholeMilliseconds)
            .apply()
    }
    
    /**
     * 获取播放位置
     */
    suspend fun getPosition(audioId: Long): Duration = withContext(Dispatchers.IO) {
        val positionMs = prefs.getLong("position_$audioId", 0L)
        positionMs.milliseconds
    }
    
    /**
     * 清除播放位置
     */
    suspend fun clearPosition(audioId: Long) = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove("position_$audioId")
            .apply()
    }
    
    /**
     * 获取所有音频的播放位置
     */
    suspend fun getAllPositions(): Map<Long, Duration> = withContext(Dispatchers.IO) {
        val positions = mutableMapOf<Long, Duration>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("position_") && value is Long) {
                val audioId = key.removePrefix("position_").toLongOrNull()
                if (audioId != null) {
                    positions[audioId] = value.milliseconds
                }
            }
        }
        positions
    }
}
