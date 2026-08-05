package com.xmvisio.app.ui.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.xmvisio.app.data.DoubleTapGesture
import com.xmvisio.app.data.LoopMode
import com.xmvisio.app.data.VideoContentScale
import com.xmvisio.app.data.VideoPlayerPreferences
import com.xmvisio.app.data.VideoPlayerPreferencesManager

/**
 * 播放器 UI 状态（与 Player 实例无关的 UI 状态）
 */
class VideoPlayerUiState {
    // ===== 控件可见性 =====
    var showControls by mutableStateOf(true)
    var activeOverlayPanel by mutableStateOf(OverlayPanelType.NONE)

    // ===== 播放状态（由 Listener 更新）=====
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var totalDuration by mutableLongStateOf(0L)
    var playbackSpeed by mutableFloatStateOf(1f)
    var loopMode by mutableStateOf(LoopMode.OFF)
    var isBuffering by mutableStateOf(false)
    var isSeeking by mutableStateOf(false)

    // ===== 错误处理 =====
    var playerError by mutableStateOf<Pair<Int, String>?>(null)

    // ===== 手势状态 =====
    var seekIndicator by mutableLongStateOf(0L)
    var showSeekIndicator by mutableStateOf(false)
    var volumeIndicator by mutableFloatStateOf(-1f)
    var brightnessIndicator by mutableFloatStateOf(-1f)
    var isLongPressing by mutableStateOf(false)
    var longPressSpeedDisplay by mutableFloatStateOf(2f)
    var doubleTapSide by mutableIntStateOf(0)
    var showDoubleTapIndicator by mutableStateOf(false)
    var dragStartPosition by mutableLongStateOf(0L)
    var accumulatedSeek by mutableFloatStateOf(0f)
    var verticalGestureSide by mutableIntStateOf(0)
    var zoomLevel by mutableFloatStateOf(1f)
    var panOffset by mutableStateOf(Offset.Zero)
    var isZooming by mutableStateOf(false)
    var showZoomIndicator by mutableStateOf(false)

    // ===== 音轨/字幕/缩放 =====
    var selectedAudioTrackIndex by mutableIntStateOf(-1)
    var selectedSubtitleIndex by mutableIntStateOf(-1)
    var selectedVideoScale by mutableStateOf(VideoContentScale.BEST_FIT)

    // ===== 音量增强 =====
    var volumeBoost by mutableFloatStateOf(1f)

    // ===== Seek 节流 =====
    val seekThrottle = SeekThrottle()

    // ===== 内部索引追踪 =====
    var internalCurrentIndex by mutableIntStateOf(0)

    // ===== 画中画 =====
    var pipReceiverRegistered by mutableStateOf(false)
    var hasUserRotated by mutableStateOf(false)

    class SeekThrottle {
        var lastSeekTime = 0L
        var pendingPosition = -1L
    }

    fun resetGestureState() {
        seekIndicator = 0L
        showSeekIndicator = false
        volumeIndicator = -1f
        brightnessIndicator = -1f
        isLongPressing = false
        doubleTapSide = 0
        showDoubleTapIndicator = false
        isZooming = false
        showZoomIndicator = false
    }
}