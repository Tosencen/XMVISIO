package com.xmvisio.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build

/**
 * 音频焦点和蓝牙耳机管理器
 * 处理音频焦点变化和蓝牙耳机连接/断开事件
 */
class AudioFocusManager(
    private val context: Context,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // 保存 AudioFocusRequest 实例（Android O+）
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    
    // 音频焦点监听器
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去音频焦点，暂停播放
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去音频焦点（如来电），暂停播放
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 暂时失去音频焦点但可以降低音量（如通知音）
                // 这里选择暂停，也可以选择降低音量
                onPause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得音频焦点
                // 注意：这里不自动恢复播放，让用户手动控制
            }
        }
    }
    
    // 蓝牙耳机断开监听器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("AudioFocusManager", "收到广播: ${intent?.action}")
            when (intent?.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    // 音频输出设备断开（如拔出耳机、断开蓝牙）
                    android.util.Log.d("AudioFocusManager", "音频设备断开，暂停播放")
                    onPause()
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    // 有线耳机插拔事件
                    val state = intent.getIntExtra("state", -1)
                    android.util.Log.d("AudioFocusManager", "耳机插拔事件: state=$state")
                    when (state) {
                        0 -> {
                            // 耳机拔出，暂停播放
                            onPause()
                        }
                        1 -> {
                            // 耳机插入，不自动播放
                        }
                    }
                }
            }
        }
    }
    
    private var isRegistered = false
    
    /**
     * 请求音频焦点
     */
    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 如果还没有创建 AudioFocusRequest，创建一个
            if (audioFocusRequest == null) {
                audioFocusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
            
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    /**
     * 放弃音频焦点
     */
    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
    
    /**
     * 注册蓝牙耳机监听器
     */
    fun register() {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isRegistered = true
            android.util.Log.d("AudioFocusManager", "已注册音频设备监听器")
        }
    }
    
    /**
     * 注销蓝牙耳机监听器
     */
    fun unregister() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
            } catch (e: Exception) {
                // 忽略重复注销的异常
            }
            isRegistered = false
        }
    }
}
