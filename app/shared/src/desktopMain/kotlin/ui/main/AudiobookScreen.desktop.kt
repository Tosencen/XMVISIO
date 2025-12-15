package com.xmvisio.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 有声书页面 - Desktop 实现
 */
@Composable
actual fun AudiobookScreen(
    onNavigateToPlayer: (Any) -> Unit,
    modifier: Modifier
) {
    // Desktop 平台使用通用实现
    AudiobookScreenCommon(modifier = modifier)
}
