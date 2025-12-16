package com.xmvisio.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlin.time.Duration

/**
 * 媒体通知管理器
 * 显示播放控制通知，包括上一首、播放/暂停、下一首按钮
 */
class MediaNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var mediaSession: MediaSessionCompat? = null
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "audio_playback_channel"
        const val CHANNEL_NAME = "音频播放"
        
        // 通知动作
        const val ACTION_PLAY = "com.xmvisio.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.xmvisio.app.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.xmvisio.app.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.xmvisio.app.ACTION_NEXT"
        const val ACTION_STOP = "com.xmvisio.app.ACTION_STOP"
    }
    
    init {
        createNotificationChannel()
        createMediaSession()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示音频播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建媒体会话
     */
    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(context, "AudioPlayerSession").apply {
            isActive = true
        }
    }
    
    /**
     * 显示或更新通知
     */
    fun showNotification(
        title: String,
        artist: String? = null,
        isPlaying: Boolean,
        sleepTimerRemaining: Duration? = null,
        isSetToAudioEnd: Boolean = false,
        hasPrevious: Boolean = true,
        hasNext: Boolean = true
    ): Notification {
        val notification = buildNotification(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            sleepTimerRemaining = sleepTimerRemaining,
            isSetToAudioEnd = isSetToAudioEnd,
            hasPrevious = hasPrevious,
            hasNext = hasNext
        )
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        return notification
    }
    
    /**
     * 构建通知
     */
    private fun buildNotification(
        title: String,
        artist: String?,
        isPlaying: Boolean,
        sleepTimerRemaining: Duration?,
        isSetToAudioEnd: Boolean,
        hasPrevious: Boolean,
        hasNext: Boolean
    ): Notification {
        // 构建内容文本（包含艺术家和倒计时信息）
        val contentText = buildString {
            artist?.let { append(it) }
            
            when {
                sleepTimerRemaining != null -> {
                    if (isNotEmpty()) append(" • ")
                    append("定时: ${formatSleepTimer(sleepTimerRemaining)}")
                }
                isSetToAudioEnd -> {
                    if (isNotEmpty()) append(" • ")
                    append("音频结束后暂停")
                }
            }
        }.ifEmpty { "正在播放" }
        
        // 点击通知打开应用
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 删除操作（滑动删除通知时暂停播放）
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            ACTION_STOP.hashCode(),
            android.content.Intent(ACTION_STOP).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知 - 使用自定义填充样式图标（无描边）
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationIcons.getSmallIcon(context))
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
        
        // 添加上一首按钮（如果没有上一首，按钮仍然显示但禁用）
        builder.addAction(
            NotificationCompat.Action.Builder(
                NotificationIcons.getSkipPreviousIcon(context),
                "上一首",
                if (hasPrevious) createPendingIntent(ACTION_PREVIOUS) else null
            ).build()
        )
        
        // 添加播放/暂停按钮
        if (isPlaying) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    NotificationIcons.getPauseIcon(context),
                    "暂停",
                    createPendingIntent(ACTION_PAUSE)
                ).build()
            )
        } else {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    NotificationIcons.getPlayIcon(context),
                    "播放",
                    createPendingIntent(ACTION_PLAY)
                ).build()
            )
        }
        
        // 添加下一首按钮（如果没有下一首，按钮仍然显示但禁用）
        builder.addAction(
            NotificationCompat.Action.Builder(
                NotificationIcons.getSkipNextIcon(context),
                "下一首",
                if (hasNext) createPendingIntent(ACTION_NEXT) else null
            ).build()
        )
        
        // 设置 MediaStyle，在紧凑视图中显示三个按钮
        builder.setStyle(
            MediaNotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2) // 显示上一首、播放/暂停、下一首
        )
        
        return builder.build()
    }
    
    /**
     * 创建 PendingIntent
     */
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * 格式化睡眠定时器显示
     */
    private fun formatSleepTimer(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 取消通知
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        cancelNotification()
        mediaSession?.release()
        mediaSession = null
    }
}
