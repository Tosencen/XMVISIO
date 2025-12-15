package com.xmvisio.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun ConfigureSystemBars(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    // iOS 通过 Info.plist 配置
}
