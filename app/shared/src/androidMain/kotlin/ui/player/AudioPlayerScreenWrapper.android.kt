package com.xmvisio.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xmvisio.app.audio.LocalAudioFile

/**
 * Android平台的音频播放器界面包装器
 */
@Composable
actual fun AudioPlayerScreenWrapper(
    audio: Any,
    onClose: () -> Unit,
    modifier: Modifier
) {
    if (audio is LocalAudioFile) {
        AudioPlayerScreen(
            audio = audio,
            onClose = onClose,
            modifier = modifier
        )
    }
}
