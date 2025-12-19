package com.xmvisio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun HandleOpenPlayerRequest(
    audioId: Long,
    onAudioFound: (Any) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(audioId) {
        // 获取当前正在播放的音频
        val playerController = com.xmvisio.app.audio.GlobalAudioPlayerController.getInstance(context)
        val currentAudio = playerController.currentAudio.value
        if (currentAudio != null && currentAudio.id == audioId) {
            onAudioFound(currentAudio)
        }
    }
}
