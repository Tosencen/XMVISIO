package com.xmvisio.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 视频播放进度管理器
 * 按视频 uri 保存 / 恢复播放进度，实现断点续播
 */
class VideoPlaybackPositionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "video_playback_positions",
        Context.MODE_PRIVATE
    )

    private fun key(uri: String): String = "pos_" + Integer.toHexString(uri.hashCode())

    fun getPosition(uri: String): Long = prefs.getLong(key(uri), 0L)

    fun savePosition(uri: String, positionMs: Long) {
        if (positionMs <= 0L) return
        prefs.edit().putLong(key(uri), positionMs).apply()
    }

    fun clearPosition(uri: String) {
        prefs.edit().remove(key(uri)).apply()
    }

    companion object {
        @Volatile
        private var instance: VideoPlaybackPositionManager? = null

        fun getInstance(context: Context): VideoPlaybackPositionManager {
            return instance ?: synchronized(this) {
                instance ?: VideoPlaybackPositionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
