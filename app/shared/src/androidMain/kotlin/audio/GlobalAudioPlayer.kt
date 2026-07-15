package com.xmvisio.app.audio

import android.content.Context

/**
 * 全局音频播放器单例
 * 确保整个应用只有一个播放器实例，避免多个音频叠加播放
 */
object GlobalAudioPlayer {
    @Volatile
    private var instance: AudioPlayer? = null
    
    @Synchronized
    fun getInstance(context: Context): AudioPlayer {
        return instance ?: AudioPlayer(context.applicationContext).also {
            instance = it
        }
    }
    
    @Synchronized
    fun release() {
        instance?.release()
        instance = null
    }
}
