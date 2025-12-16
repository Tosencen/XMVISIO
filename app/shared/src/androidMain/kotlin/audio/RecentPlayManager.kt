package com.xmvisio.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 最近播放管理器
 * 记录和管理最近播放的音频
 */
class RecentPlayManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("recent_play", Context.MODE_PRIVATE)
    
    private val _recentAudioId = MutableStateFlow<Long?>(null)
    val recentAudioId: StateFlow<Long?> = _recentAudioId.asStateFlow()
    
    companion object {
        private const val KEY_RECENT_AUDIO_ID = "recent_audio_id"
        private const val KEY_RECENT_AUDIO_TITLE = "recent_audio_title"
        private const val KEY_RECENT_PLAY_TIME = "recent_play_time"
        
        @Volatile
        private var instance: RecentPlayManager? = null
        
        fun getInstance(context: Context): RecentPlayManager {
            return instance ?: synchronized(this) {
                instance ?: RecentPlayManager(context.applicationContext).also { 
                    instance = it
                    it.loadRecentAudio()
                }
            }
        }
    }
    
    /**
     * 记录最近播放的音频
     */
    suspend fun recordRecentPlay(audioId: Long, title: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putLong(KEY_RECENT_AUDIO_ID, audioId)
            .putString(KEY_RECENT_AUDIO_TITLE, title)
            .putLong(KEY_RECENT_PLAY_TIME, System.currentTimeMillis())
            .apply()
        
        _recentAudioId.value = audioId
    }
    
    /**
     * 获取最近播放的音频ID
     */
    fun getRecentAudioId(): Long? {
        val audioId = prefs.getLong(KEY_RECENT_AUDIO_ID, -1L)
        return if (audioId != -1L) audioId else null
    }
    
    /**
     * 获取最近播放的音频标题
     */
    fun getRecentAudioTitle(): String? {
        return prefs.getString(KEY_RECENT_AUDIO_TITLE, null)
    }
    
    /**
     * 加载最近播放的音频
     */
    private fun loadRecentAudio() {
        _recentAudioId.value = getRecentAudioId()
    }
    
    /**
     * 清除最近播放记录
     */
    suspend fun clearRecentPlay() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        _recentAudioId.value = null
    }
}
