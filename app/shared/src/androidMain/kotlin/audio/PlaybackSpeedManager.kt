package com.xmvisio.app.audio

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 播放速度管理器
 * 用于全局保存和恢复播放速度设置
 */
class PlaybackSpeedManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "playback_speed_settings")
        private val PLAYBACK_SPEED_KEY = floatPreferencesKey("playback_speed")
        private const val DEFAULT_SPEED = 1.0f
    }
    
    /**
     * 获取播放速度 Flow
     */
    val playbackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PLAYBACK_SPEED_KEY] ?: DEFAULT_SPEED
    }
    
    /**
     * 保存播放速度
     */
    suspend fun saveSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED_KEY] = speed
        }
    }
    
    /**
     * 获取当前播放速度（同步方法）
     */
    suspend fun getSpeed(): Float {
        var speed = DEFAULT_SPEED
        context.dataStore.data.collect { preferences ->
            speed = preferences[PLAYBACK_SPEED_KEY] ?: DEFAULT_SPEED
        }
        return speed
    }
}
