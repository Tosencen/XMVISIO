package com.xmvisio.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xmvisio.app.data.VideoInfo

@Composable
expect fun VideoPlayerScreen(
    video: VideoInfo,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)
