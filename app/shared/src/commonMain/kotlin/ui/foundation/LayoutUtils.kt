package com.xmvisio.app.ui.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 简化的布局工具
@Composable
fun currentWindowAdaptiveInfo1(): WindowAdaptiveInfo {
    return WindowAdaptiveInfo()
}

class WindowAdaptiveInfo {
    val windowSizeClass = WindowSizeClass()
}

class WindowSizeClass {
    val cardVerticalPadding: Dp = 8.dp
}

val WindowSizeClass.cardVerticalPadding: Dp
    get() = 8.dp
