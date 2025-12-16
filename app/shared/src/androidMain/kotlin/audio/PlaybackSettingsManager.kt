package com.xmvisio.app.audio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放设置管理器
 * 管理跳过空白和音量提升设置
 */
class PlaybackSettingsManager(context: Context) {
    
    private val prefs = context.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
    
    private val _skipSilence = MutableStateFlow(getSkipSilence())
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()
    
    private val _volumeBoost = MutableStateFlow(getVolumeBoost())
    val volumeBoost: StateFlow<Float> = _volumeBoost.asStateFlow()
    
    companion object {
        private const val KEY_SKIP_SILENCE = "skip_silence"
        private const val KEY_VOLUME_BOOST = "volume_boost"
        const val MAX_VOLUME_BOOST = 9f // 最大音量提升（分贝）
        
        @Volatile
        private var instance: PlaybackSettingsManager? = null
        
        fun getInstance(context: Context): PlaybackSettingsManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackSettingsManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    /**
     * 获取跳过空白设置
     */
    fun getSkipSilence(): Boolean {
        return prefs.getBoolean(KEY_SKIP_SILENCE, false)
    }
    
    /**
     * 设置跳过空白
     */
    fun setSkipSilence(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_SILENCE, enabled).apply()
        _skipSilence.value = enabled
    }
    
    /**
     * 获取音量提升（分贝）
     */
    fun getVolumeBoost(): Float {
        return prefs.getFloat(KEY_VOLUME_BOOST, 0f)
    }
    
    /**
     * 设置音量提升（分贝）
     */
    fun setVolumeBoost(decibel: Float) {
        val clamped = decibel.coerceIn(0f, MAX_VOLUME_BOOST)
        prefs.edit().putFloat(KEY_VOLUME_BOOST, clamped).apply()
        _volumeBoost.value = clamped
    }
}
