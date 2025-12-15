package com.xmvisio.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun ConfigureSystemBars(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    // Desktop 不需要配置系统栏
}
