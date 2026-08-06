package com.xmvisio.app.data

import androidx.compose.runtime.Immutable

@Immutable
data class SubtitlePreferences(
    // 使用系统字幕样式（跟随系统无障碍字幕设置）
    val useSystemCaptionStyle: Boolean = false,
    // 字幕文字大小（sp）
    val textSize: Int = 20,
    // 文字加粗
    val textBold: Boolean = false,
    // 显示字幕背景
    val background: Boolean = true,
    // 应用字幕内嵌样式（字幕文件自带样式）
    val applyEmbeddedStyles: Boolean = true,
) {
    companion object {
        val DEFAULT = SubtitlePreferences()
    }
}
