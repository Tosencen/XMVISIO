package com.xmvisio.app

import androidx.compose.runtime.Composable

@Composable
actual fun HandleOpenPlayerRequest(
    audioId: Long,
    onAudioFound: (Any) -> Unit
) {
    // Desktop 平台不需要处理通知打开播放器的逻辑
}
