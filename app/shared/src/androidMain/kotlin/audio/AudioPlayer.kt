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
    private val speedManager = PlaybackSpeedManager(context)
    
    // 音频焦点管理器
    private val audioFocusManager = AudioFocusManager(
        context = context,
        onPause = { pause() },
        onResume = { /* 不自动恢复播放，让用户手动控制 */ }
    )
    
    // 播放列表和自动播放下一首
    private var playlist: List<Uri> = emptyList()
    private var playlistIds: List<Long> = emptyList()
    private var onPlayNextCallback: ((Long) -> Unit)? = null
    
    init {
        // 注册音频焦点和蓝牙耳机监听器
        audioFocusManager.register()
        
        // 初始化时加载保存的播放速度
        CoroutineScope(Dispatchers.IO).launch {
            speedManager.playbackSpeed.collect { speed ->
                _playbackSpeed.value = speed
                // 如果播放器已经准备好，应用速度
                mediaPlayer?.let { player ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = player.playbackParams.setSpeed(speed)
                        } catch (e: Exception) {
                            println("应用播放速度失败: ${e.message}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取当前播放的音频ID
     */
    fun getCurrentAudioId(): Long? = _currentAudioId.value
    
    /**
     * 同步播放状态（用于应用恢复时）
     */
    fun syncPlaybackState() {
        mediaPlayer?.let { player ->
            try {
                val actuallyPlaying = player.isPlaying
                if (_isPlaying.value != actuallyPlaying) {
                    _isPlaying.value = actuallyPlaying
                    println("同步播放状态: isPlaying=$actuallyPlaying")
                }
            } catch (e: Exception) {
                println("同步播放状态失败: ${e.message}")
            }
        }
    }
    
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
            
            // 确保音频焦点监听器已注册（可能在之前的 release 中被注销）
            audioFocusManager.register()
            
            // 停止并释放当前播放器（不注销监听器）
            releaseMediaPlayerOnly()
            currentUri = uri
            _currentAudioId.value = audioId
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    _duration.value = mp.duration.milliseconds
                    
                    // 应用保存的播放速度
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(_playbackSpeed.value)
                        } catch (e: Exception) {
                            println("应用播放速度失败: ${e.message}")
                        }
                    }
                    
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
                    
                    // 检查睡眠定时器是否设置为音频结束时暂停
                    val sleepTimerManager = SleepTimerManager.getInstance(context)
                    val shouldPauseAtEnd = sleepTimerManager.checkAndPauseAtAudioEnd()
                    
                    if (shouldPauseAtEnd) {
                        // 显示提示
                        showToast("当前音频已播放完成，已自动暂停")
                    } else {
                        // 尝试播放下一首
                        val hasNext = playNext()
                        if (!hasNext) {
                            // 没有下一首了，显示提示
                            showToast("播放列表已全部播放完成")
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
        // 请求音频焦点
        if (!audioFocusManager.requestAudioFocus()) {
            println("无法获取音频焦点")
            return
        }
        
        mediaPlayer?.let {
            try {
                if (!it.isPlaying) {
                    it.start()
                }
                // 无论如何都更新状态，确保UI同步
                _isPlaying.value = true
            } catch (e: Exception) {
                println("播放失败: ${e.message}")
                _isPlaying.value = false
            }
        }
    }
    
    /**
     * 暂停
     */
    fun pause() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                }
                // 无论如何都更新状态，确保UI同步
                _isPlaying.value = false
            } catch (e: Exception) {
                println("暂停失败: ${e.message}")
            }
        }
        
        // 暂停时放弃音频焦点
        audioFocusManager.abandonAudioFocus()
    }
    
    /**
     * 切换播放/暂停
     */
    fun togglePlayPause() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    audioFocusManager.abandonAudioFocus()
                } else {
                    // 请求音频焦点
                    if (audioFocusManager.requestAudioFocus()) {
                        it.start()
                        _isPlaying.value = true
                    }
                }
            } catch (e: Exception) {
                println("切换播放状态失败: ${e.message}")
            }
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
     * 设置播放速度（全局保存）
     */
    fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    _playbackSpeed.value = speed
                    
                    // 保存到全局设置
                    CoroutineScope(Dispatchers.IO).launch {
                        speedManager.saveSpeed(speed)
                    }
                } catch (e: Exception) {
                    // 某些设备可能不支持
                    println("设置播放速度失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 设置播放列表
     */
    fun setPlaylist(uris: List<Uri>, ids: List<Long>, onPlayNext: (Long) -> Unit) {
        playlist = uris
        playlistIds = ids
        onPlayNextCallback = onPlayNext
    }
    
    /**
     * 播放下一首
     * @return 是否有下一首
     */
    private fun playNext(): Boolean {
        // 优先使用 GlobalAudioPlayerController 来播放下一首
        // 这样即使界面关闭，也能继续自动播放
        try {
            val controller = GlobalAudioPlayerController.getInstance(context)
            controller.playNext()
            return true
        } catch (e: Exception) {
            // 如果 GlobalAudioPlayerController 不可用，使用回调方式
            val currentId = _currentAudioId.value ?: return false
            val currentIndex = playlistIds.indexOf(currentId)
            
            if (currentIndex >= 0 && currentIndex < playlistIds.size - 1) {
                // 有下一首，通知回调
                val nextId = playlistIds[currentIndex + 1]
                onPlayNextCallback?.invoke(nextId)
                return true
            }
            return false
        }
    }
    
    /**
     * 显示 Toast 提示
     */
    private fun showToast(message: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            android.widget.Toast.makeText(
                context,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
     * 仅释放 MediaPlayer（不注销监听器，用于切换音频时）
     */
    private fun releaseMediaPlayerOnly() {
        // 保存当前位置
        _currentAudioId.value?.let { id ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                positionManager.savePosition(id, _currentPosition.value)
            }
        }
        
        audioFocusManager.abandonAudioFocus()
        
        mediaPlayer?.release()
        mediaPlayer = null
        currentUri = null
        _currentAudioId.value = null
        _isPlaying.value = false
        _currentPosition.value = Duration.ZERO
        _duration.value = Duration.ZERO
    }
    
    /**
     * 释放资源（完全释放，包括注销监听器）
     */
    fun release() {
        // 保存当前位置
        _currentAudioId.value?.let { id ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                positionManager.savePosition(id, _currentPosition.value)
            }
        }
        
        // 注销音频焦点和蓝牙耳机监听器
        audioFocusManager.unregister()
        audioFocusManager.abandonAudioFocus()
        
        mediaPlayer?.release()
        mediaPlayer = null
        currentUri = null
        _currentAudioId.value = null
        _isPlaying.value = false
        _currentPosition.value = Duration.ZERO
        _duration.value = Duration.ZERO
    }
}
