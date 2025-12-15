package com.xmvisio.app.audio

import android.content.Context

/**
 * 全局音频播放器单例
 * 确保整个应用只有一个播放器实例，避免多个音频叠加播放
 */
object GlobalAudioPlayer {
    private var instance: AudioPlayer? = null
    
    fun getInstance(context: Context): AudioPlayer {
        if (instance == null) {
            instance = AudioPlayer(context.applicationContext)
        }
        return instance!!
    }
    
    fun release() {
        instance?.release()
        instance = null
    }
}
