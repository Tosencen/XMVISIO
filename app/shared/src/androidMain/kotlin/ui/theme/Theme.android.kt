package com.xmvisio.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Android 平台主题实现
 * 支持：Android 12+ 动态主题、Material Kolor 多色主题
 */
@Composable
actual fun appColorScheme(
    seedColor: Color,
    useDynamicTheme: Boolean,
    useBlackBackground: Boolean,
    isDark: Boolean
): ColorScheme {
    val context = LocalContext.current
    
    // Android 12+ 系统动态主题（从壁纸提取）
    if (useDynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        // 动态主题也要应用黑色背景
        return applyBlackBackground(scheme, isDark, useBlackBackground)
    }
    
    // Android 12 以下或未启用动态主题：使用 Material Kolor 从种子颜色生成配色
    val scheme = dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = useBlackBackground,
        style = PaletteStyle.TonalSpot,
    )
    
    return applyBlackBackground(scheme, isDark, useBlackBackground)
}

/**
 * 应用纯黑背景
 */
private fun applyBlackBackground(
    colorScheme: ColorScheme,
    isDark: Boolean,
    useBlackBackground: Boolean
): ColorScheme {
    return if (isDark && useBlackBackground) {
        colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceContainer = Color(0xFF1C1B1F),
            surfaceContainerLow = Color(0xFF1C1B1F),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerLowest = Color.Black
        )
    } else {
        colorScheme
    }
}

/**
 * Android 平台支持动态主题
 * - Android 12+ (API 31+)：使用系统动态主题（从壁纸提取）
 * - Android 12 以下：使用 Material Kolor 基于种子颜色生成动态配色
 */
@Composable
actual fun isPlatformSupportDynamicTheme(): Boolean {
    return true
}
