package com.xmvisio.app.audio

import android.content.Context

/**
 * 通知图标资源ID获取器
 * 用于在shared模块中访问android模块的drawable资源
 */
object NotificationIcons {
    fun getSkipPreviousIcon(context: Context): Int {
        return context.resources.getIdentifier(
            "ic_skip_previous_filled",
            "drawable",
            context.packageName
        )
    }
    
    fun getPlayIcon(context: Context): Int {
        return context.resources.getIdentifier(
            "ic_play_arrow_filled",
            "drawable",
            context.packageName
        )
    }
    
    fun getPauseIcon(context: Context): Int {
        return context.resources.getIdentifier(
            "ic_pause_filled",
            "drawable",
            context.packageName
        )
    }
    
    fun getSkipNextIcon(context: Context): Int {
        return context.resources.getIdentifier(
            "ic_skip_next_filled",
            "drawable",
            context.packageName
        )
    }
    
    fun getSmallIcon(context: Context): Int {
        return context.resources.getIdentifier(
            "ic_notification_small",
            "drawable",
            context.packageName
        )
    }
}
