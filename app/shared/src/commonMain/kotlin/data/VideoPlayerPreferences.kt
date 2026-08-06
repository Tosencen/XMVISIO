package com.xmvisio.app.data

import androidx.compose.runtime.Immutable

enum class DoubleTapGesture {
    PLAY_PAUSE, FAST_FORWARD_AND_REWIND, BOTH, NONE
}

enum class LoopMode {
    OFF, ONE, ALL
}

enum class VideoContentScale {
    BEST_FIT, STRETCH, CROP, HUNDRED_PERCENT
}

enum class ScreenOrientation {
    AUTOMATIC, LANDSCAPE, LANDSCAPE_REVERSE, LANDSCAPE_AUTO, PORTRAIT, VIDEO_ORIENTATION
}

@Immutable
data class VideoPlayerPreferences(
    // 播放
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val loopMode: LoopMode = LoopMode.OFF,
    val resumePlayback: Boolean = true,

    // 手势控制
    val enableSeekGesture: Boolean = true,
    val seekSensitivity: Float = 0.5f,
    val enableVolumeGesture: Boolean = true,
    val volumeSensitivity: Float = 0.5f,
    val enableBrightnessGesture: Boolean = true,
    val brightnessSensitivity: Float = 0.5f,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    // 默认开启长按快进（此前默认 false 导致长按无反应，用户反馈的 bug）
    val enableLongPress: Boolean = true,
    val longPressSpeed: Float = 2.0f,
    val seekIncrement: Int = 10,

    // 缩放手势
    val enableZoomGesture: Boolean = true,
    val enablePanGesture: Boolean = false,

    // 界面
    val controllerAutoHideTimeout: Int = 4,
    val controlsLocked: Boolean = false,
    val screenOrientation: ScreenOrientation = ScreenOrientation.AUTOMATIC,
    val videoContentScale: VideoContentScale = VideoContentScale.BEST_FIT,
) {
    companion object {
        val DEFAULT = VideoPlayerPreferences()
    }
}
