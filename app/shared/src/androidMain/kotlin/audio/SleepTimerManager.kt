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
import kotlin.time.Duration.Companion.milliseconds

/**
 * 全局睡眠定时器管理器
 * 作用于整个有声书功能
 */
class SleepTimerManager private constructor(private val context: Context) {
    
    private val _endTime = MutableStateFlow<Long?>(null)
    val endTime: StateFlow<Long?> = _endTime.asStateFlow()
    
    private val _remainingTime = MutableStateFlow<Duration?>(null)
    val remainingTime: StateFlow<Duration?> = _remainingTime.asStateFlow()
    
    private var timerJob: Job? = null
    
    /**
     * 设置睡眠定时器
     */
    fun setTimer(duration: Duration) {
        val endTimeMillis = System.currentTimeMillis() + duration.inWholeMilliseconds
        _endTime.value = endTimeMillis
        
        // 取消之前的定时器
        timerJob?.cancel()
        
        // 启动新的定时器
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (System.currentTimeMillis() < endTimeMillis) {
                val remaining = (endTimeMillis - System.currentTimeMillis()).milliseconds
                _remainingTime.value = remaining
                delay(1000) // 每秒更新一次
            }
            
            // 时间到，暂停播放
            onTimerExpired()
        }
    }
    
    /**
     * 取消睡眠定时器
     */
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _endTime.value = null
        _remainingTime.value = null
    }
    
    /**
     * 定时器到期时的回调
     */
    private fun onTimerExpired() {
        // 暂停全局播放器
        GlobalAudioPlayer.getInstance(context).pause()
        
        // 清除定时器状态
        _endTime.value = null
        _remainingTime.value = null
        timerJob = null
    }
    
    companion object {
        @Volatile
        private var instance: SleepTimerManager? = null
        
        fun getInstance(context: Context): SleepTimerManager {
            return instance ?: synchronized(this) {
                instance ?: SleepTimerManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
