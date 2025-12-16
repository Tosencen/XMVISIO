package com.xmvisio.app.audio

import android.content.Context

/**
 * 全局音频管理器单例
 */
object GlobalAudioManager {
    private var instance: AudioManager? = null
    
    fun initialize(context: Context, activity: androidx.activity.ComponentActivity) {
        if (instance == null) {
            instance = AudioManager(context.applicationContext).apply {
                initialize(activity)
            }
        }
    }
    
    fun getInstance(context: Context): AudioManager {
        return instance ?: AudioManager(context.applicationContext)
    }
}
