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
    
    // 缓存 PendingIntent，避免重复创建导致失效（特别是在 Android 15 后台场景）
    private val previousPendingIntent: PendingIntent by lazy { createPendingIntent(ACTION_PREVIOUS) }
    private val playPendingIntent: PendingIntent by lazy { createPendingIntent(ACTION_PLAY) }
    private val pausePendingIntent: PendingIntent by lazy { createPendingIntent(ACTION_PAUSE) }
    private val nextPendingIntent: PendingIntent by lazy { createPendingIntent(ACTION_NEXT) }
    
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
        hasNext: Boolean = true,
        audioId: Long? = null
    ): Notification {
        println("MediaNotificationManager: showNotification() - title=\"$title\", isPlaying=$isPlaying, hasPrevious=$hasPrevious, hasNext=$hasNext")
        
        val notification = buildNotification(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            sleepTimerRemaining = sleepTimerRemaining,
            isSetToAudioEnd = isSetToAudioEnd,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            audioId = audioId
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
        hasNext: Boolean,
        audioId: Long?
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
        
        // 点击通知打开播放器详情页
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            // 添加标志，确保使用现有的 Activity 实例
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 传递音频 ID，用于打开播放器
            audioId?.let { putExtra("OPEN_PLAYER_AUDIO_ID", it) }
        }
        
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
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
        
        // 使用缓存的 PendingIntent，确保在后台快速更新通知时不会失效
        // 添加上一首按钮（始终启用，点击时再判断）
        builder.addAction(
            NotificationCompat.Action.Builder(
                NotificationIcons.getSkipPreviousIcon(context),
                "上一首",
                previousPendingIntent
            ).build()
        )
        
        // 添加播放/暂停按钮 - 使用缓存的 PendingIntent
        if (isPlaying) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    NotificationIcons.getPauseIcon(context),
                    "暂停",
                    pausePendingIntent
                ).build()
            )
        } else {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    NotificationIcons.getPlayIcon(context),
                    "播放",
                    playPendingIntent
                ).build()
            )
        }
        
        // 添加下一首按钮（始终启用，点击时再判断）
        builder.addAction(
            NotificationCompat.Action.Builder(
                NotificationIcons.getSkipNextIcon(context),
                "下一首",
                nextPendingIntent
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
     * 使用 FLAG_IMMUTABLE 确保 PendingIntent 不会被修改
     * 使用唯一的 requestCode 确保每个 action 都有独立的 PendingIntent
     */
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            // 添加 action 作为 intent 的一部分，确保唯一性
            setAction(action)
        }
        // 使用固定的 requestCode（action 的 hashCode），确保相同 action 复用同一个 PendingIntent
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
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
