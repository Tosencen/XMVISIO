package com.xmvisio.app.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class DarkMode {
    AUTO, LIGHT, DARK
}

@Immutable
data class ThemeSettings(
    val darkMode: DarkMode = DarkMode.AUTO,
    val useDynamicTheme: Boolean = false,
    val useBlackBackground: Boolean = false,
    val seedColorValue: ULong = DefaultSeedColor.value,
) {
    val seedColor: Color = Color(seedColorValue).let {
        if (it == Color.Unspecified) DefaultSeedColor else it
    }

    companion object {
        val Default = ThemeSettings()
    }
}

// 默认种子颜色 - 紫色
val DefaultSeedColor = Color(0xFF6750A4)
