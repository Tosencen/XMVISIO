package com.xmvisio.app.data.player

import kotlin.time.Duration

/**
 * 音频播放状态
 */
data class AudioPlayState(
    val audioId: Long,
    val title: String,
    val artist: String?,
    val duration: Duration,
    val playedTime: Duration,
    val playing: Boolean
)
