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
    private val recentPlayManager = RecentPlayManager.getInstance(context)
    
    private val _currentAudio = MutableStateFlow<LocalAudioFile?>(null)
    val currentAudio: StateFlow<LocalAudioFile?> = _currentAudio.asStateFlow()
    
    private val _playlist = MutableStateFlow<List<LocalAudioFile>>(emptyList())
    val playlist: StateFlow<List<LocalAudioFile>> = _playlist.asStateFlow()
    
    /**
     * 获取当前播放的音频（同步方法，供UI直接访问）
     */
    fun getCurrentAudio(): LocalAudioFile? = _currentAudio.value
    
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
        
        // 同时更新 AudioPlayer 的播放列表，确保后台播放时也能正确切换
        audioPlayer.setPlaylist(
            uris = playlist.map { it.uri },
            ids = playlist.map { it.id },
            onPlayNext = { nextId ->
                // 当 AudioPlayer 自动播放下一首时，更新 currentAudio
                val nextAudio = playlist.find { it.id == nextId }
                if (nextAudio != null) {
                    _currentAudio.value = nextAudio
                }
            }
        )
        
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
            
            // 记录最近播放
            CoroutineScope(Dispatchers.Main).launch {
                recentPlayManager.recordRecentPlay(previousAudio.id, previousAudio.title)
            }
            
            // 准备并播放
            CoroutineScope(Dispatchers.Main).launch {
                audioPlayer.prepare(
                    uri = previousAudio.uri,
                    audioId = previousAudio.id,
                    onPrepared = {
                        // 先更新 currentAudio，确保通知显示正确的标题
                        _currentAudio.value = previousAudio
                        // 然后开始播放
                        audioPlayer.play()
                        // 手动触发通知更新，确保标题和按钮状态都正确
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
            
            // 记录最近播放
            CoroutineScope(Dispatchers.Main).launch {
                recentPlayManager.recordRecentPlay(nextAudio.id, nextAudio.title)
            }
            
            // 准备并播放
            CoroutineScope(Dispatchers.Main).launch {
                audioPlayer.prepare(
                    uri = nextAudio.uri,
                    audioId = nextAudio.id,
                    onPrepared = {
                        // 先更新 currentAudio，确保通知显示正确的标题
                        _currentAudio.value = nextAudio
                        // 然后开始播放
                        audioPlayer.play()
                        // 手动触发通知更新
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
        val audio = _currentAudio.value ?: run {
            println("GlobalAudioPlayerController: updateNotification() - currentAudio is null, skipping")
            return
        }
        
        // 优先从 AudioPlayer 获取播放列表（更可靠）
        val list = if (_playlist.value.isEmpty()) {
            // 如果 GlobalAudioPlayerController 的播放列表为空，尝试从 AudioPlayer 重建
            emptyList<LocalAudioFile>()
        } else {
            _playlist.value
        }
        
        val currentIndex = list.indexOfFirst { it.id == audio.id }
        val isPlaying = audioPlayer.isPlaying.value
        
        println("GlobalAudioPlayerController: updateNotification() - title=\"${audio.title}\", isPlaying=$isPlaying")
        
        notificationManager.showNotification(
            title = audio.title,
            artist = audio.artist,
            isPlaying = isPlaying,
            sleepTimerRemaining = sleepTimerManager.remainingTime.value,
            isSetToAudioEnd = sleepTimerManager.isSetToAudioEnd.value,
            hasPrevious = currentIndex > 0,
            hasNext = currentIndex < list.size - 1,
            audioId = audio.id
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
