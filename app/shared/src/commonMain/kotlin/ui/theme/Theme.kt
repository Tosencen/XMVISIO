package com.xmvisio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.xmvisio.app.data.DarkMode
import com.xmvisio.app.data.DefaultSeedColor
import com.xmvisio.app.data.ThemeSettings

/**
 * 主题设置 Context
 * 复刻自 Animeko
 */
val LocalThemeSettings = compositionLocalOf {
    ThemeSettings.Default
}

/**
 * 根据主题设置生成配色方案
 * 支持：动态主题、自定义种子颜色、黑色背景
 */
@Composable
expect fun appColorScheme(
    seedColor: Color = LocalThemeSettings.current.seedColor,
    useDynamicTheme: Boolean = LocalThemeSettings.current.useDynamicTheme,
    useBlackBackground: Boolean = LocalThemeSettings.current.useBlackBackground,
    isDark: Boolean = when (LocalThemeSettings.current.darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.AUTO -> isSystemInDarkTheme()
    },
): ColorScheme

/**
 * 平台是否支持动态主题
 */
@Composable
expect fun isPlatformSupportDynamicTheme(): Boolean

/**
 * 应用主题
 * 复刻自 Animeko 的 AniTheme
 */
@Composable
fun AppTheme(
    themeSettings: ThemeSettings = ThemeSettings.Default,
    darkModeOverride: DarkMode? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (darkModeOverride ?: themeSettings.darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.AUTO -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = appColorScheme(
            seedColor = themeSettings.seedColor,
            useDynamicTheme = themeSettings.useDynamicTheme,
            useBlackBackground = themeSettings.useBlackBackground,
            isDark = isDark
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}
