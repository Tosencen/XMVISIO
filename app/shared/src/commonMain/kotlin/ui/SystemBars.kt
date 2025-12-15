package com.xmvisio.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 跨平台系统栏配置
 */
@Composable
expect fun ConfigureSystemBars(
    isDark: Boolean = false,
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent
)
