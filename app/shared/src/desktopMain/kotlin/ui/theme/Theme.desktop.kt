package com.xmvisio.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Desktop 平台主题实现
 * 使用 Material Kolor 从种子颜色生成配色
 */
@Composable
actual fun appColorScheme(
    seedColor: Color,
    useDynamicTheme: Boolean,
    useBlackBackground: Boolean,
    isDark: Boolean
): ColorScheme {
    // Desktop 不支持系统动态主题，使用 Material Kolor
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
 * Desktop 支持基于种子颜色的动态主题（使用 Material Kolor）
 * 注意：不支持从系统壁纸提取颜色（仅 Android 12+ 支持）
 */
@Composable
actual fun isPlatformSupportDynamicTheme(): Boolean {
    return true
}
