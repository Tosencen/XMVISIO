package com.xmvisio.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 媒体通知广播接收器
 * 处理通知栏的播放控制按钮点击事件
 */
class MediaNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val audioPlayer = GlobalAudioPlayer.getInstance(context)
        
        when (intent.action) {
            MediaNotificationManager.ACTION_PLAY -> {
                audioPlayer.play()
            }
            MediaNotificationManager.ACTION_PAUSE -> {
                audioPlayer.pause()
            }
            MediaNotificationManager.ACTION_PREVIOUS -> {
                // 播放上一首
                val controller = GlobalAudioPlayerController.getInstance(context)
                controller.playPrevious()
            }
            MediaNotificationManager.ACTION_NEXT -> {
                // 播放下一首
                val controller = GlobalAudioPlayerController.getInstance(context)
                controller.playNext()
            }
            MediaNotificationManager.ACTION_STOP -> {
                // 停止播放并关闭通知
                audioPlayer.pause()
                val notificationManager = MediaNotificationManager(context)
                notificationManager.cancelNotification()
            }
        }
    }
}
