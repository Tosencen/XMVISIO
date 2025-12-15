package com.xmvisio.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * Desktop 标题栏 insets
 */
expect val LocalTitleBarInsets: ProvidableCompositionLocal<WindowInsets>
