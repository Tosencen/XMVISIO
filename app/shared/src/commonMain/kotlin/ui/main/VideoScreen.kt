package com.xmvisio.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xmvisio.app.data.VideoInfo

@Composable
expect fun VideoScreen(
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    updateAvailable: Boolean = false,
    onUpdateCheck: () -> Unit = {},
    modifier: Modifier = Modifier
)
