package com.xmvisio.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android 平台的有声书页面
 */
@Composable
actual fun AudiobookScreen(
    onNavigateToPlayer: (Any) -> Unit,
    updateAvailable: Boolean,
    onUpdateCheck: () -> Unit,
    modifier: Modifier
) {
    AudiobookScreenImpl(
        onNavigateToPlayer = { audio ->
            onNavigateToPlayer(audio)
        },
        updateAvailable = updateAvailable,
        onUpdateCheck = onUpdateCheck,
        modifier = modifier
    )
}
