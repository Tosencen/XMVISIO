package com.xmvisio.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android 平台的有声书页面
 */
@Composable
actual fun AudiobookScreen(modifier: Modifier) {
    AudiobookScreenImpl(modifier = modifier)
}
