package com.xmvisio.app.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * 全局音频播放器控制器
 * 管理播放列表、通知和播放状态
 */
class GlobalAudioPlayerController private constructor(private val context: Context) {
    
    private val audioPlayer = GlobalAudioPlayer.getInstance(context)
    private val notificationManager = MediaNotificationManager(context)
    private val sleepTimerManager = SleepTimerManager.getInstance(context)
    
    private val _currentAudio = MutableStateFlow<LocalAudioFile?>(null)
    val currentAudio: StateFlow<LocalAudioFile?> = _currentAudio.asStateFlow()
    
    private val _playlist = MutableStateFlow<List<LocalAudioFile>>(emptyList())
    val playlist: StateFlow<List<LocalAudioFile>> = _playlist.asStateFlow()
    
    private var notificationUpdateJob: Job? = null
    
    init {
        // 监听播放状态变化，更新通知
        CoroutineScope(Dispatchers.Main).launch {
            audioPlayer.isPlaying.collect { isPlaying ->
                updateNotification()
                
                // 如果正在播放，启动定时更新通知（用于更新倒计时）
                if (isPlaying) {
                    startNotificationUpdates()
                } else {
                    stopNotificationUpdates()
                }
            }
        }
        
        // 监听睡眠定时器变化，更新通知
        CoroutineScope(Dispatchers.Main).launch {
            sleepTimerManager.remainingTime.collect {
                updateNotification()
            }
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            sleepTimerManager.isSetToAudioEnd.collect {
                updateNotification()
            }
        }
    }
    
    /**
     * 设置当前播放的音频和播放列表
     */
    fun setCurrentAudio(audio: LocalAudioFile, playlist: List<LocalAudioFile>) {
        _currentAudio.value = audio
        _playlist.value = playlist
        updateNotification()
    }
    
    /**
     * 播放上一首
     */
    fun playPrevious() {
        val current = _currentAudio.value ?: return
        val list = _playlist.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        
        if (currentIndex > 0) {
            val previousAudio = list[currentIndex - 1]
            _currentAudio.value = previousAudio
            
            // 准备并播放
            CoroutineScope(Dispatchers.Main).launch {
                audioPlayer.prepare(
                    uri = previousAudio.uri,
                    audioId = previousAudio.id,
                    onPrepared = {
                        audioPlayer.play()
                        // 确保播放开始后更新通知
                        updateNotification()
                    }
                )
            }
        }
    }
    
    /**
     * 播放下一首
     */
    fun playNext() {
        val current = _currentAudio.value ?: return
        val list = _playlist.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        
        if (currentIndex >= 0 && currentIndex < list.size - 1) {
            val nextAudio = list[currentIndex + 1]
            _currentAudio.value = nextAudio
            
            // 准备并播放
            CoroutineScope(Dispatchers.Main).launch {
                audioPlayer.prepare(
                    uri = nextAudio.uri,
                    audioId = nextAudio.id,
                    onPrepared = {
                        audioPlayer.play()
                        // 确保播放开始后更新通知
                        updateNotification()
                    }
                )
            }
        }
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val audio = _currentAudio.value ?: return
        val list = _playlist.value
        val currentIndex = list.indexOfFirst { it.id == audio.id }
        
        notificationManager.showNotification(
            title = audio.title,
            artist = audio.artist,
            isPlaying = audioPlayer.isPlaying.value,
            sleepTimerRemaining = sleepTimerManager.remainingTime.value,
            isSetToAudioEnd = sleepTimerManager.isSetToAudioEnd.value,
            hasPrevious = currentIndex > 0,
            hasNext = currentIndex < list.size - 1
        )
    }
    
    /**
     * 启动定时更新通知（用于更新倒计时）
     */
    private fun startNotificationUpdates() {
        stopNotificationUpdates()
        notificationUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000) // 每秒更新一次
                updateNotification()
            }
        }
    }
    
    /**
     * 停止定时更新通知
     */
    private fun stopNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
    }
    
    /**
     * 取消通知
     */
    fun cancelNotification() {
        stopNotificationUpdates()
        notificationManager.cancelNotification()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopNotificationUpdates()
        notificationManager.release()
    }
    
    companion object {
        @Volatile
        private var instance: GlobalAudioPlayerController? = null
        
        fun getInstance(context: Context): GlobalAudioPlayerController {
            return instance ?: synchronized(this) {
                instance ?: GlobalAudioPlayerController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
