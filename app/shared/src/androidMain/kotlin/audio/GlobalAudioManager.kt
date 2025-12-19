package com.xmvisio.app.audio

import android.content.Context
import androidx.activity.ComponentActivity

/**
 * 全局音频管理器单例
 * 在 MainActivity.onCreate 中初始化
 */
object GlobalAudioManager {
    private var audioManager: AudioManager? = null
    
    /**
     * 在 Activity.onCreate 中调用，初始化 AudioManager
     * 每次 Activity onCreate 时都会重新初始化，确保 ActivityResultLauncher 有效
     */
    fun initialize(context: Context, activity: ComponentActivity) {
        // 每次都重新初始化，因为 Activity 可能被 recreate
        audioManager = AudioManager(context).apply {
            initialize(activity)
        }
    }
    
    /**
     * 获取 AudioManager 实例
     */
    fun getInstance(context: Context): AudioManager {
        return audioManager ?: throw IllegalStateException(
            "GlobalAudioManager not initialized. Call initialize() in MainActivity.onCreate first."
        )
    }
}
