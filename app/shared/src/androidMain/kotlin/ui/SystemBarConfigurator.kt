package com.xmvisio.app.ui

import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.Color

/**
 * 系统栏配置工具
 * 集中管理系统栏颜色和样式配置逻辑
 */
object SystemBarConfigurator {
    /**
     * 将 Compose Color 转换为 Android Color Int
     */
    fun Color.toAndroidColor(): Int {
        return android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
    
    /**
     * 根据主题配置创建状态栏样式
     */
    fun createStatusBarStyle(
        isDark: Boolean,
        statusBarColor: Color
    ): SystemBarStyle {
        val colorInt = statusBarColor.toAndroidColor()
        return if (isDark) {
            SystemBarStyle.dark(colorInt)
        } else {
            SystemBarStyle.light(colorInt, colorInt)
        }
    }
    
    /**
     * 创建导航栏样式（始终透明）
     */
    fun createNavigationBarStyle(isDark: Boolean): SystemBarStyle {
        return if (isDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        }
    }
}
