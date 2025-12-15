package com.xmvisio.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf

/**
 * Desktop 标题栏 insets（红绿黄按钮区域）
 */
actual val LocalTitleBarInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }
