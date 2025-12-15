package com.xmvisio.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 音频播放器界面包装器（平台特定实现）
 */
@Composable
expect fun AudioPlayerScreenWrapper(
    audio: Any,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)
