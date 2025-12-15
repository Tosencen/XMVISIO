package com.xmvisio.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf

/**
 * iOS 不需要标题栏 insets
 */
actual val LocalTitleBarInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }
