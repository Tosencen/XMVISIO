package com.xmvisio.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 音频播放器界面包装器 - Desktop 实现
 */
@Composable
actual fun AudioPlayerScreenWrapper(
    audio: Any,
    onClose: () -> Unit,
    modifier: Modifier
) {
    // Desktop 平台的简单占位实现
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Desktop Audio Player (Coming Soon)",
            style = MaterialTheme.typography.titleLarge
        )
    }
}
