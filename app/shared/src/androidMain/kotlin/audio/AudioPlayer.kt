package com.xmvisio.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 音频播放器（封装MediaPlayer）
 */
class AudioPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: Uri? = null
    
    private val _currentAudioId = MutableStateFlow<Long?>(null)
    val currentAudioId: StateFlow<Long?> = _currentAudioId.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(Duration.ZERO)
    val currentPosition: StateFlow<Duration> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(Duration.ZERO)
    val duration: StateFlow<Duration> = _duration.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private val positionManager = PlaybackPositionManager(context)
    
    /**
     * 获取当前播放的音频ID
     */
    fun getCurrentAudioId(): Long? = _currentAudioId.value
    
    /**
     * 准备播放
     */
    suspend fun prepare(
        uri: Uri,
        audioId: Long,
        onPrepared: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            // 如果正在播放相同的音频，不需要重新准备
            if (_currentAudioId.value == audioId && currentUri == uri && mediaPlayer != null) {
                onPrepared()
                return
            }
            
            // 停止并释放当前播放器
            release()
            currentUri = uri
            _currentAudioId.value = audioId
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    _duration.value = mp.duration.milliseconds
                    
                    // 恢复上次播放位置
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val savedPosition = positionManager.getPosition(audioId)
                        if (savedPosition > Duration.ZERO && savedPosition < mp.duration.milliseconds) {
                            mp.seekTo(savedPosition.inWholeMilliseconds.toInt())
                            _currentPosition.value = savedPosition
                        }
                        onPrepared()
                    }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = Duration.ZERO
                    // 播放完成后清除保存的位置
                    _currentAudioId.value?.let { id ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            positionManager.clearPosition(id)
                        }
                    }
                }
                setOnErrorListener { _, what, extra ->
                    onError(Exception("MediaPlayer error: what=$what, extra=$extra"))
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    /**
     * 播放
     */
    fun play() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
            }
        }
    }
    
    /**
     * 暂停
     */
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }
    
    /**
     * 切换播放/暂停
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Duration) {
        mediaPlayer?.let {
            val positionMs = position.inWholeMilliseconds.toInt()
            it.seekTo(positionMs)
            _currentPosition.value = position
        }
    }
    
    /**
     * 快退
     */
    fun rewind(seconds: Int) {
        mediaPlayer?.let {
            val currentMs = it.currentPosition
            val newPosition = (currentMs - seconds * 1000).coerceAtLeast(0)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition.milliseconds
        }
    }
    
    /**
     * 快进
     */
    fun fastForward(seconds: Int) {
        mediaPlayer?.let {
            val currentMs = it.currentPosition
            val durationMs = it.duration
            val newPosition = (currentMs + seconds * 1000).coerceAtMost(durationMs)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition.milliseconds
        }
    }
    
    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Duration {
        return mediaPlayer?.currentPosition?.milliseconds ?: Duration.ZERO
    }
    
    /**
     * 更新当前位置（用于UI更新）
     */
    fun updateCurrentPosition() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                _currentPosition.value = it.currentPosition.milliseconds
                
                // 每次更新位置时保存到本地（每秒保存一次）
                _currentAudioId.value?.let { id ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        positionManager.savePosition(id, _currentPosition.value)
                    }
                }
            }
        }
    }
    
    /**
     * 设置播放速度
     */
    fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    _playbackSpeed.value = speed
                } catch (e: Exception) {
                    // 某些设备可能不支持
                    println("设置播放速度失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 暂停并保存位置
     */
    suspend fun pauseAndSave() {
        pause()
        _currentAudioId.value?.let { id ->
            positionManager.savePosition(id, _currentPosition.value)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        // 保存当前位置
        _currentAudioId.value?.let { id ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                positionManager.savePosition(id, _currentPosition.value)
            }
        }
        
        mediaPlayer?.release()
        mediaPlayer = null
        currentUri = null
        _currentAudioId.value = null
        _isPlaying.value = false
        _currentPosition.value = Duration.ZERO
        _duration.value = Duration.ZERO
    }
}
