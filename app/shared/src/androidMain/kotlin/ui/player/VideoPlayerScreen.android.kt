package com.xmvisio.app.ui.player

import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Icon
import android.net.Uri
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import com.xmvisio.app.ui.player.extensions.detectCustomHorizontalDragGestures
import com.xmvisio.app.ui.player.extensions.detectCustomVerticalDragGestures
import com.xmvisio.app.ui.player.extensions.detectCustomTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.view.View
import androidx.media3.common.C
import com.xmvisio.app.data.DoubleTapGesture
import com.xmvisio.app.data.LoopMode
import com.xmvisio.app.data.ScreenOrientation
import com.xmvisio.app.data.VideoContentScale
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.data.VideoPlayerPreferencesManager
import com.xmvisio.app.data.SubtitlePreferencesManager
import com.xmvisio.app.data.VideoPlaybackPositionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayerScreen(
    video: VideoInfo,
    videos: List<VideoInfo>,
    currentIndex: Int,
    onClose: () -> Unit,
    onNavigateToVideo: (Int) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { VideoPlayerPreferencesManager.getInstance(context) }
    val prefs by prefsManager.preferences.collectAsState()
    val subtitlePrefsManager = remember { SubtitlePreferencesManager.getInstance(context) }
    val subtitlePrefs by subtitlePrefsManager.preferences.collectAsState()
    val positionManager = remember { VideoPlaybackPositionManager.getInstance(context) }
    val sliderStyleManager = remember { com.xmvisio.app.data.SliderStyleManager.getInstance(context) }
    val sliderStyle by sliderStyleManager.sliderStyle.collectAsState()

    // 当前播放的视频索引（内部追踪，不触发 player 重建）
    var internalCurrentIndex by remember { mutableIntStateOf(currentIndex) }

    // Player 只创建一次，不复建（参考 NextPlayer：通过 setMediaItems + seekToNext/Previous 切换视频）
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    // 初始化播放列表并开始播放
    LaunchedEffect(videos.hashCode()) {
        val mediaItems = videos.map { v ->
            MediaItem.fromUri(Uri.parse(v.uri))
        }
        player.setMediaItems(mediaItems, currentIndex, C.TIME_UNSET)
        // 断点续播：进入时恢复上次播放进度
        if (prefs.resumePlayback) {
            val startUri = videos.getOrNull(currentIndex)?.uri
            val saved = startUri?.let { positionManager.getPosition(it) } ?: 0L
            if (saved > 0L) {
                player.seekTo(saved)
            }
        }
        player.prepare()
        player.playWhenReady = prefs.autoplay
        player.setPlaybackSpeed(prefs.defaultPlaybackSpeed)
        player.repeatMode = when (prefs.loopMode) {
            LoopMode.OFF -> Player.REPEAT_MODE_OFF
            LoopMode.ONE -> Player.REPEAT_MODE_ONE
            LoopMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // === 音量增强器（LoudnessEnhancer）===
    var volumeBoost by remember { mutableFloatStateOf(1f) } // 1f = 原始音量
    val loudnessEnhancer = remember { mutableStateOf<LoudnessEnhancer?>(null) }

    LaunchedEffect(player) {
        // 等待 AudioSessionId 可用后创建 LoudnessEnhancer；
        // 若播放中 sessionId 变化（如切换音轨），重建以增强器
        var lastSessionId = C.AUDIO_SESSION_ID_UNSET
        while (isActive) {
            val sessionId = player.audioSessionId
            if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != lastSessionId) {
                try {
                    loudnessEnhancer.value?.release()
                    loudnessEnhancer.value = LoudnessEnhancer(sessionId).apply {
                        enabled = true
                        setTargetGain(((volumeBoost - 1f) * 1000f).toInt())
                    }
                    lastSessionId = sessionId
                } catch (_: Exception) {}
            }
            delay(1000)
        }
    }

    // 当 volumeBoost 变化时更新 LoudnessEnhancer
    LaunchedEffect(volumeBoost) {
        val gain = ((volumeBoost - 1f) * 1000f).toInt().coerceIn(0, 2000)
        loudnessEnhancer.value?.setTargetGain(gain)
    }

    DisposableEffect(Unit) {
        onDispose { loudnessEnhancer.value?.release() }
    }

    // === 播放状态 ===
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(prefs.defaultPlaybackSpeed) }
    var loopMode by remember { mutableStateOf(prefs.loopMode) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }

    // === 错误处理 ===
    var playerError by remember { mutableStateOf<Pair<Int, String>?>(null) }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING && !isSeeking
                // 播放完成：清除该视频的续播进度，下次从头开始
                if (state == Player.STATE_ENDED) {
                    videos.getOrNull(player.currentMediaItemIndex)?.uri
                        ?.let { positionManager.clearPosition(it) }
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex
                if (idx in videos.indices) {
                    internalCurrentIndex = idx
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playerError = Pair(error.errorCode, error.localizedMessage ?: "播放出错")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Seek 快进模式：seek 时使用 CLOSEST_SYNC（最近关键帧），实现丝滑拖拽
    // 拖拽结束后恢复 DEFAULT（精确 seek）。Media3 1.6.x 不支持 isScrubbingModeEnabled，
    // 用 SeekParameters 切换实现等价效果（参考 NextPlayer 思路）
    var savedSeekParameters by remember { mutableStateOf<SeekParameters?>(null) }
    LaunchedEffect(isSeeking) {
        if (isSeeking) {
            savedSeekParameters = player.seekParameters
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        } else {
            savedSeekParameters?.let { player.setSeekParameters(it) }
            savedSeekParameters = null
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            currentPosition = player.currentPosition
            totalDuration = player.duration
            delay(200)
        }
    }

    // 断点续播：每隔 3 秒保存当前视频的播放进度
    LaunchedEffect(player) {
        while (isActive) {
            delay(3000)
            if (!prefs.resumePlayback) continue
            val idx = player.currentMediaItemIndex
            val uri = videos.getOrNull(idx)?.uri ?: continue
            val dur = player.duration
            val pos = player.currentPosition
            // 仅在播放中段（>5s 且距结尾 >3s）保存，避免保存开头/结尾无意义进度
            if (dur > 0 && pos > 5000 && pos < dur - 3000) {
                positionManager.savePosition(uri, pos)
            }
        }
    }

    // 移除旧的 400ms 固定时长过渡遮罩，改用 coverSurface 驱动 —— 更精确
    // （coverSurface 是 rememberPresentationState 提供的，当 surface 准备好渲染首帧时自动变 false）

    // === 手势状态 ===
    var seekIndicator by remember { mutableStateOf(0L) } // seek 量（毫秒），正=前进，负=后退
    var showSeekIndicator by remember { mutableStateOf(false) }
    var volumeIndicator by remember { mutableFloatStateOf(-1f) } // -1 = 不显示
    var brightnessIndicator by remember { mutableFloatStateOf(-1f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressSpeedDisplay by remember { mutableFloatStateOf(2f) }

    // 双击状态
    var doubleTapSide by remember { mutableStateOf(0) } // -1=左, 0=中, 1=右
    var showDoubleTapIndicator by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableLongStateOf(0L) } // seek 起始位置
    var accumulatedSeek by remember { mutableFloatStateOf(0f) } // seek 累计位移（px×灵敏度）
    var verticalGestureSide by remember { mutableIntStateOf(0) } // -1=亮度侧, 0=无, 1=音量侧

    // === 双指缩放/平移状态 ===
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isZooming by remember { mutableStateOf(false) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    // Seek 节流：避免每帧都调用 player.seekTo() 导致卡顿
    val seekThrottle = remember { object {
        var lastSeekTime = 0L
        var pendingPosition = -1L
    } }

    // 侧滑面板状态
    var activeOverlayPanel by remember { mutableStateOf(OverlayPanelType.NONE) }
    var selectedAudioTrackIndex by remember { mutableIntStateOf(-1) } // -1 = 关闭
    var selectedSubtitleIndex by remember { mutableIntStateOf(-1) } // -1 = 关闭
    var selectedVideoScale by remember { mutableStateOf(prefs.videoContentScale) }

    // 控件自动隐藏
    val autoHideTimeout = prefs.controllerAutoHideTimeout * 1000L
    LaunchedEffect(showControls, isLongPressing) {
        if (showControls && !isLongPressing && autoHideTimeout > 0) {
            delay(autoHideTimeout)
            showControls = false
        }
    }

    // 双击指示器自动隐藏
    LaunchedEffect(showDoubleTapIndicator) {
        if (showDoubleTapIndicator) {
            delay(600)
            showDoubleTapIndicator = false
            isSeeking = false
        }
    }

    // Seek 指示器自动隐藏
    LaunchedEffect(showSeekIndicator) {
        if (showSeekIndicator) {
            delay(600)
            showSeekIndicator = false
            isSeeking = false
        }
    }

    // 缩放指示器自动隐藏
    LaunchedEffect(showZoomIndicator) {
        if (showZoomIndicator) {
            delay(1000)
            showZoomIndicator = false
        }
    }

    // 当视频缩放模式改变时重置 zoom/pan
    LaunchedEffect(selectedVideoScale) {
        zoomLevel = 1f
        panOffset = Offset.Zero
    }

    // 音量控制
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // === 音频焦点处理 ===
    DisposableEffect(player) {
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> player.pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.volume = 0.3f
                AudioManager.AUDIOFOCUS_GAIN -> {
                    player.volume = 1f
                    if (prefs.autoplay) player.play()
                }
            }
        }
        audioManager.requestAudioFocus(
            focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        onDispose { audioManager.abandonAudioFocus(focusListener) }
    }

    // 亮度控制
    val window = (context as? android.app.Activity)?.window
    val layoutParams = window?.attributes
    val activity = context as? android.app.Activity

    // === PiP RemoteAction（画中画操作按钮）===
    val pipIntentAction = "com.xmvisio.app.PIP_ACTION"
    val pipActionPlay = "PIP_PLAY"
    val pipActionPause = "PIP_PAUSE"
    val pipActionNext = "PIP_NEXT"
    val pipActionPrevious = "PIP_PREVIOUS"
    val pipActionCodes = mapOf(
        pipActionPlay to 1, pipActionPause to 2,
        pipActionNext to 3, pipActionPrevious to 4
    )

    val pipReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getIntExtra("pip_action_code", -1)) {
                    1 -> player.play()
                    2 -> player.pause()
                    3 -> player.seekToNextMediaItem()
                    4 -> player.seekToPreviousMediaItem()
                }
            }
        }
    }

    // 进入 PiP 时注册 receiver，退出时注销（用标志位防止重复注册）
    var pipReceiverRegistered by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        onDispose {
            if (pipReceiverRegistered) {
                try { activity?.unregisterReceiver(pipReceiver) } catch (_: Exception) {}
                pipReceiverRegistered = false
            }
        }
    }

    fun createPipAction(code: Int, title: String, icon: PipIcon): RemoteAction? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val intent = Intent(pipIntentAction).apply {
            putExtra("pip_action_code", code)
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(
            createPipIcon(context, icon),
            title, title, pendingIntent
        )
    }

    fun enterPiP() {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            // 注册 BroadcastReceiver 以响应 PiP 操作（仅注册一次，避免重复注册抛异常）
            if (!pipReceiverRegistered) {
                context.registerReceiver(
                    pipReceiver,
                    IntentFilter(pipIntentAction),
                    Context.RECEIVER_NOT_EXPORTED
                )
                pipReceiverRegistered = true
            }
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOfNotNull(
                    createPipAction(4, "上一个", PipIcon.PREV),
                    if (player.isPlaying)
                        createPipAction(2, "暂停", PipIcon.PAUSE)
                    else
                        createPipAction(1, "播放", PipIcon.PLAY),
                    createPipAction(3, "下一个", PipIcon.NEXT),
                ))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (_: Exception) {}
    }

    // === 屏幕方向状态（RotationState） ===
    val rotationState = rememberRotationState(
        player = player,
        screenOrientation = prefs.screenOrientation,
    )
    var hasUserRotated by remember { mutableStateOf(false) }

    // 进入播放页面时设置方向，退出时恢复（仅首次，此后由 RotationState 管理）
    DisposableEffect(Unit) {
        val act = activity
        rotationState.applyOrientation(act)
        // 播放视频时保持屏幕常亮
        act?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 状态栏图标设为白色（视频场景）
        val windowInsetsController = act?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        }
        windowInsetsController?.isAppearanceLightStatusBars = false
        windowInsetsController?.isAppearanceLightNavigationBars = false
        onDispose {
            rotationState.restoreOrientation(act)
            act?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // 恢复状态栏图标为系统默认
            windowInsetsController?.isAppearanceLightStatusBars = true
            windowInsetsController?.isAppearanceLightNavigationBars = true
        }
    }

    // 监听视频尺寸变化（VIDEO_ORIENTATION 模式时动态调整）
    LaunchedEffect(player) {
        rotationState.observeVideoSize(activity)
    }

    BackHandler {
        if (prefs.controlsLocked) return@BackHandler
        
        // 如果用户手动旋转过，先恢复预设方向
        if (hasUserRotated) {
            hasUserRotated = false
            rotationState.applyOrientation(activity)
            return@BackHandler
        }
        
        if (showControls) onClose() else showControls = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // === 视频画面 + 手势层 ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 双指缩放手势（优先级高于单指手势，放在外层）
                .pointerInput(prefs.enableZoomGesture, prefs.enablePanGesture) {
                    if (!prefs.enableZoomGesture) return@pointerInput
                    detectCustomTransformGestures(
                        onTransform = { pan, zoomChange ->
                            if (player.duration == C.TIME_UNSET) return@detectCustomTransformGestures
                            isZooming = true
                            zoomLevel = (zoomLevel * zoomChange).coerceIn(0.25f, 4f)

                            if (prefs.enablePanGesture) {
                                val constraints = androidx.compose.ui.unit.Constraints(
                                    maxWidth = size.width,
                                    maxHeight = size.height
                                )
                                val extraWidth = (zoomLevel - 1) * constraints.maxWidth
                                val extraHeight = (zoomLevel - 1) * constraints.maxHeight
                                val maxX = kotlin.math.abs(extraWidth / 2).toFloat()
                                val maxY = kotlin.math.abs(extraHeight / 2).toFloat()

                                panOffset = Offset(
                                    x = (panOffset.x + zoomLevel * pan.x).coerceIn(-maxX, maxX),
                                    y = (panOffset.y + zoomLevel * pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                            showZoomIndicator = true
                        },
                        onTransformEnd = {
                            isZooming = false
                        }
                    )
                }
                // 单指手势层
                .pointerInput(prefs.enableSeekGesture, prefs.enableVolumeGesture, prefs.enableBrightnessGesture, prefs.controlsLocked, prefs.enableLongPress, prefs.longPressSpeed) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            if (isLongPressing) {
                                isLongPressing = false
                                player.setPlaybackSpeed(playbackSpeed)
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (prefs.controlsLocked || prefs.doubleTapGesture == DoubleTapGesture.NONE) return@detectTapGestures
                            val centerX = size.width / 2f
                            when (prefs.doubleTapGesture) {
                                DoubleTapGesture.PLAY_PAUSE -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                }
                                DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                                    val seekMs = prefs.seekIncrement * 1000L
                                    isSeeking = true
                                    if (offset.x < centerX) {
                                        player.seekTo((player.currentPosition - seekMs).coerceAtLeast(0))
                                        seekIndicator = -seekMs
                                        doubleTapSide = -1
                                    } else {
                                        player.seekTo((player.currentPosition + seekMs).coerceAtMost(player.duration))
                                        seekIndicator = seekMs
                                        doubleTapSide = 1
                                    }
                                    showDoubleTapIndicator = true
                                }
                                DoubleTapGesture.BOTH -> {
                                    if (abs(offset.x - centerX) < size.width * 0.15f) {
                                        if (player.isPlaying) player.pause() else player.play()
                                    } else {
                                        val seekMs = prefs.seekIncrement * 1000L
                                        isSeeking = true
                                        if (offset.x < centerX) {
                                            player.seekTo((player.currentPosition - seekMs).coerceAtLeast(0))
                                            seekIndicator = -seekMs
                                            doubleTapSide = -1
                                        } else {
                                            player.seekTo((player.currentPosition + seekMs).coerceAtMost(player.duration))
                                            seekIndicator = seekMs
                                            doubleTapSide = 1
                                        }
                                        showDoubleTapIndicator = true
                                    }
                                }
                                else -> {}
                            }
                        },
                        onLongPress = {
                            val cur = prefsManager.preferences.value
                            if (prefs.controlsLocked || !cur.enableLongPress) return@detectTapGestures
                            isLongPressing = true
                            // 直接从 manager 读取最新值，避免闭包捕获到旧的 prefs 快照
                            //（设置里改了长按速度后，此处必须拿到最新值）
                            longPressSpeedDisplay = cur.longPressSpeed
                            player.setPlaybackSpeed(cur.longPressSpeed)
                            showControls = false
                        }
                    )
                }
                // === 水平拖动手势（Seek 快进/快退）===
                .pointerInput(prefs.enableSeekGesture, prefs.controlsLocked) {
                    if (!prefs.enableSeekGesture || prefs.controlsLocked) return@pointerInput

                    detectCustomHorizontalDragGestures(
                        onDragStart = {
                            dragStartPosition = player.currentPosition
                            accumulatedSeek = 0f
                            seekIndicator = 0L
                            seekThrottle.lastSeekTime = 0L
                            seekThrottle.pendingPosition = -1L
                            isSeeking = true
                        },
                        onDragEnd = {
                            // 关键：先恢复精确 seek 参数，再做最后一次 seek。
                            // 否则仍使用 CLOSEST_SYNC（最近关键帧），最终进度会被吸附到
                            // 关键帧上，与指示器显示的位置不符（跟手但进度不准）。
                            savedSeekParameters?.let { player.setSeekParameters(it) }
                                ?: player.setSeekParameters(SeekParameters.DEFAULT)
                            savedSeekParameters = null
                            // 松开时执行最后一次（精确）seek
                            if (seekThrottle.pendingPosition >= 0) {
                                player.seekTo(seekThrottle.pendingPosition)
                            }
                            showSeekIndicator = false
                            isSeeking = false
                        },
                        onDragCancel = {
                            savedSeekParameters?.let { player.setSeekParameters(it) }
                                ?: player.setSeekParameters(SeekParameters.DEFAULT)
                            savedSeekParameters = null
                            showSeekIndicator = false
                            isSeeking = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (isLongPressing) return@detectCustomHorizontalDragGestures

                            val sensitivity = prefs.seekSensitivity
                            accumulatedSeek += dragAmount * sensitivity * 50f
                            val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
                            val newPos = (dragStartPosition + accumulatedSeek.toLong()).coerceIn(0L, duration)
                            seekIndicator = accumulatedSeek.toLong()
                            isSeeking = true
                            showSeekIndicator = true

                            // 节流：每 100ms 最多 seek 一次
                            val now = System.currentTimeMillis()
                            seekThrottle.pendingPosition = newPos
                            if (now - seekThrottle.lastSeekTime >= 100) {
                                seekThrottle.lastSeekTime = now
                                player.seekTo(newPos)
                            }
                        }
                    )
                }
                // === 垂直拖动手势（亮度 / 音量）===
                .pointerInput(prefs.enableBrightnessGesture, prefs.enableVolumeGesture, prefs.controlsLocked) {
                    if (prefs.controlsLocked) return@pointerInput

                    detectCustomVerticalDragGestures(
                        onDragStart = { offset ->
                            val thirdWidth = size.width / 3f
                            verticalGestureSide = when {
                                offset.x < thirdWidth && prefs.enableBrightnessGesture -> -1 // 左侧 = 亮度
                                offset.x > size.width - thirdWidth && prefs.enableVolumeGesture -> 1 // 右侧 = 音量
                                else -> 0 // 中间区域不响应
                            }
                        },
                        onDragEnd = {
                            volumeIndicator = -1f
                            brightnessIndicator = -1f
                        },
                        onDragCancel = {
                            volumeIndicator = -1f
                            brightnessIndicator = -1f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (isLongPressing) return@detectCustomVerticalDragGestures

                            when (verticalGestureSide) {
                                -1 -> { // 左侧 = 亮度
                                    val sensitivity = prefs.brightnessSensitivity
                                    val delta = -dragAmount * sensitivity * 0.01f
                                    val currentBrightness = layoutParams?.screenBrightness ?: 0.5f
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                                    layoutParams?.let {
                                        it.screenBrightness = newBrightness
                                        window.attributes = it
                                    }
                                    brightnessIndicator = newBrightness
                                }
                                1 -> { // 右侧 = 音量
                                    val sensitivity = prefs.volumeSensitivity
                                    val delta = -dragAmount * sensitivity * 0.5f
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val newVol = (currentVol + delta.roundToInt()).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    volumeIndicator = newVol.toFloat() / maxVolume
                                }
                            }
                        }
                    )
                }
        ) {
            val resizeMode = when (selectedVideoScale) {
                VideoContentScale.BEST_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoContentScale.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoContentScale.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoContentScale.HUNDRED_PERCENT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomLevel
                        scaleY = zoomLevel
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                // 受控的 PlayerView：禁用自带控制器与内置字幕，
                // 改用自定义 SubtitleView 叠加渲染（样式可配置）
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val pv = PlayerView(ctx)
                        pv.useController = false
                        pv.subtitleView?.visibility = View.GONE
                        pv.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        pv.setResizeMode(resizeMode)
                        pv.player = player
                        pv
                    },
                    update = { view ->
                        view.setResizeMode(resizeMode)
                        if (view.player == null) view.player = player
                    }
                )

                // 自定义字幕渲染（样式由字幕偏好控制）
                SubtitleView(
                    player = player,
                    prefs = subtitlePrefs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // === Seek 指示器 ===
        if (showSeekIndicator) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = formatSeekDelta(seekIndicator),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }

        // === 缓冲指示器 ===
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }

        // === 双击指示器 ===
        if (showDoubleTapIndicator) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (doubleTapSide > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formatSeekDelta(seekIndicator),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // === 音量指示器（水平居中，画面中上方） ===
        if (volumeIndicator >= 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = volumeIndicator)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .align(Alignment.CenterStart)
                        )
                    }
                    Text("${(volumeIndicator * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // === 亮度指示器（水平居中，画面中上方） ===
        if (brightnessIndicator >= 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(top = 130.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Brightness6, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = brightnessIndicator)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .align(Alignment.CenterStart)
                        )
                    }
                    Text("${(brightnessIndicator * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // === 长按快进指示器 ===
        if (isLongPressing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "长按快进 ${longPressSpeedDisplay.toInt()}x",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        // === 缩放指示器 ===
        if (showZoomIndicator || zoomLevel != 1f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 96.dp)
                ) {
                    Text(
                        text = "缩放 ${(zoomLevel * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // === 锁定状态：锁按钮（放在控件层右上角） ===
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { prefsManager.setControlsLocked(!prefs.controlsLocked) },
                    modifier = Modifier.padding(end = 8.dp).size(40.dp)
                ) {
                    Icon(
                        imageVector = if (prefs.controlsLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                        contentDescription = if (prefs.controlsLocked) "解锁" else "锁定",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // === 控件层（锁定时不显示） ===
        AnimatedVisibility(
            visible = showControls && !prefs.controlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // 顶部栏
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerActionButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            onClick = { if (!prefs.controlsLocked) onClose() }
                        )
                        Text(
                            text = videos.getOrNull(internalCurrentIndex)?.name ?: video.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).basicMarquee(),
                        )
                        PlayerActionButton(Icons.Default.ClosedCaption, "字幕") {
                            if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.SUBTITLE) OverlayPanelType.NONE else OverlayPanelType.SUBTITLE
                        }
                        PlayerActionButton(Icons.Default.Tune, "音轨") {
                            if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.AUDIO_TRACK) OverlayPanelType.NONE else OverlayPanelType.AUDIO_TRACK
                        }
                        Box {
                            PlayerActionButton(Icons.Default.Speed, "速度") {
                                if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.PLAYBACK_SPEED) OverlayPanelType.NONE else OverlayPanelType.PLAYBACK_SPEED
                            }
                        }
                    }
                }

                // 中间播放控制
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!prefs.controlsLocked) {
                                player.seekToPreviousMediaItem()
                                onNavigateToVideo(player.currentMediaItemIndex)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一个",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (!prefs.controlsLocked) { if (player.isPlaying) player.pause() else player.play() } },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!prefs.controlsLocked) {
                                player.seekToNextMediaItem()
                                onNavigateToVideo(player.currentMediaItemIndex)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一个",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 底部栏
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .navigationBarsPadding()
                        .offset(y = (-14).dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // 进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        when (sliderStyle) {
                            com.xmvisio.app.data.SliderStyle.SQUIGGLY -> {
                                val fraction = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                                me.saket.squiggles.SquigglySlider(
                                    value = fraction,
                                    onValueChange = { f ->
                                        if (!prefs.controlsLocked) {
                                            isSeeking = true
                                            player.seekTo((f * totalDuration).toLong())
                                        }
                                    },
                                    onValueChangeFinished = { isSeeking = false },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }
                            else -> {
                                Slider(
                                    value = currentPosition.toFloat().coerceIn(0f, totalDuration.coerceAtLeast(1L).toFloat()),
                                    valueRange = 0f..totalDuration.coerceAtLeast(1L).toFloat(),
                                    onValueChange = { value ->
                                        if (!prefs.controlsLocked) {
                                            isSeeking = true
                                            player.seekTo(value.toLong())
                                        }
                                    },
                                    onValueChangeFinished = { isSeeking = false },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    thumb = {
                                        if (sliderStyle != com.xmvisio.app.data.SliderStyle.SLIM) {
                                            SliderDefaults.Thumb(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                colors = SliderDefaults.colors(thumbColor = Color.White)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        Text(
                            text = formatDuration(totalDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 底部操作按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerActionButton(Icons.Default.AspectRatio, "缩放", onClick = {
                            if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.VIDEO_SCALE) OverlayPanelType.NONE else OverlayPanelType.VIDEO_SCALE
                        })
                        PlayerActionButton(Icons.Default.PictureInPicture, "画中画", onClick = {
                            if (!prefs.controlsLocked) enterPiP()
                        })
                        // 循环模式
                        PlayerActionButton(
                            icon = when (loopMode) {
                                LoopMode.OFF -> Icons.Default.Repeat
                                LoopMode.ONE -> Icons.Default.RepeatOne
                                LoopMode.ALL -> Icons.Default.Repeat
                            },
                            contentDescription = when (loopMode) {
                                LoopMode.OFF -> "循环：关"
                                LoopMode.ONE -> "循环：单曲"
                                LoopMode.ALL -> "循环：列表"
                            },
                            iconSize = 28,
                            tint = if (loopMode != LoopMode.OFF) MaterialTheme.colorScheme.primary else Color.White,
                            onClick = {
                                if (!prefs.controlsLocked) {
                                    loopMode = when (loopMode) {
                                        LoopMode.OFF -> LoopMode.ONE
                                        LoopMode.ONE -> LoopMode.ALL
                                        LoopMode.ALL -> LoopMode.OFF
                                    }
                                    prefsManager.setLoopMode(loopMode)
                                    player.repeatMode = when (loopMode) {
                                        LoopMode.OFF -> Player.REPEAT_MODE_OFF
                                        LoopMode.ONE -> Player.REPEAT_MODE_ONE
                                        LoopMode.ALL -> Player.REPEAT_MODE_ALL
                                    }
                                }
                            }
                        )
                        // 音量增强按钮
                        PlayerActionButton(
                            icon = Icons.Default.VolumeUp,
                            contentDescription = "音量增强: ${(volumeBoost * 100).toInt()}%",
                            iconSize = 28,
                            tint = if (volumeBoost > 1f) MaterialTheme.colorScheme.primary else Color.White,
                            onClick = {
                                if (!prefs.controlsLocked) {
                                    volumeBoost = when (volumeBoost) {
                                        1f -> 1.5f
                                        1.5f -> 2f
                                        else -> 1f
                                    }
                                }
                            }
                        )
                        PlayerActionButton(Icons.Default.PlaylistPlay, "播放列表", iconSize = 28, onClick = {
                            if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.PLAYLIST) OverlayPanelType.NONE else OverlayPanelType.PLAYLIST
                        })
                        // 播放列表放在主 Box 外部的 AlertDialog 中
                        PlayerActionButton(
                            icon = Icons.Default.ScreenRotation,
                            contentDescription = "旋转",
                            onClick = {
                                if (!prefs.controlsLocked) {
                                    hasUserRotated = true
                                    rotationState.rotate(activity)
                                }
                            }
                        )
                    }
                }
            }
        }

        // === 侧滑面板 ===
        VideoPlayerOverlayPanel(
            panelType = activeOverlayPanel,
            onDismiss = { activeOverlayPanel = OverlayPanelType.NONE }
        ) {
            when (activeOverlayPanel) {
                OverlayPanelType.AUDIO_TRACK -> AudioTrackPanel(
                    player = player,
                    selectedAudioTrackIndex = selectedAudioTrackIndex,
                    onTrackSelected = { index ->
                        selectedAudioTrackIndex = index
                        switchAudioTrack(player, index)
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                OverlayPanelType.SUBTITLE -> SubtitlePanel(
                    player = player,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                    onTrackSelected = { index ->
                        selectedSubtitleIndex = index
                        switchSubtitleTrack(player, index)
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                OverlayPanelType.PLAYBACK_SPEED -> PlaybackSpeedPanel(
                    currentSpeed = playbackSpeed,
                    onSpeedSelected = {
                        playbackSpeed = it
                        player.setPlaybackSpeed(it)
                        prefsManager.setDefaultPlaybackSpeed(it)
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                OverlayPanelType.VIDEO_SCALE -> VideoScalePanel(
                    currentScale = selectedVideoScale,
                    onScaleSelected = {
                        selectedVideoScale = it
                        prefsManager.setVideoContentScale(it)
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                OverlayPanelType.PLAYLIST -> PlaylistPanel(
                    videos = videos,
                    currentIndex = internalCurrentIndex,
                    onVideoSelected = { index ->
                        player.seekToDefaultPosition(index)
                        player.playWhenReady = true
                        onNavigateToVideo(index)
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                else -> {}
            }
        }
    }

    // === 错误弹窗 ===
    playerError?.let { (code, message) ->
        AlertDialog(
            onDismissRequest = { playerError = null },
            title = { Text("播放错误") },
            text = {
                Text("错误码: $code\n$message")
            },
            confirmButton = {
                TextButton(onClick = {
                    // 尝试播放下一个视频
                    playerError = null
                    player.seekToNextMediaItem()
                    player.playWhenReady = true
                }) {
                    Text("播放下一个")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerError = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int = 48,
    iconSize: Int = 24,
    enabled: Boolean = true,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize.dp),
            tint = tint
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatSeekDelta(deltaMs: Long): String {
    val absSeconds = (kotlin.math.abs(deltaMs) / 1000).toInt()
    val sign = if (deltaMs > 0) "+" else "-"
    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val seconds = absSeconds % 60
    return if (hours > 0) "$sign%d:%02d:%02d".format(hours, minutes, seconds)
    else "$sign%02d:%02d".format(minutes, seconds)
}

// === 轨道切换工具函数 ===

/**
 * 切换音频轨道。
 * @param index 轨道在列表中的索引，-1 表示关闭（静音）
 */
private fun switchAudioTrack(player: Player, index: Int) {
    if (index == -1) {
        player.volume = 0f
        return
    }
    player.volume = 1f
    val tracks = player.currentTracks
    var audioGroupIndex = -1
    var audioTrackIndex = 0
    var count = 0
    for (gi in tracks.groups.indices) {
        val group = tracks.groups[gi]
        if (group.type == C.TRACK_TYPE_AUDIO && group.isSupported) {
            for (ti in 0 until group.length) {
                if (count == index) {
                    audioGroupIndex = gi
                    audioTrackIndex = ti
                }
                count++
            }
        }
    }
    if (audioGroupIndex >= 0) {
        val group = tracks.groups[audioGroupIndex]
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .addOverride(
                androidx.media3.common.TrackSelectionOverride(
                    group.mediaTrackGroup, listOf(audioTrackIndex)
                )
            )
            .build()
    }
}

/**
 * 切换字幕轨道。
 * @param index 轨道在列表中的索引，-1 表示关闭字幕
 */
private fun switchSubtitleTrack(player: Player, index: Int) {
    if (index == -1) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
        return
    }
    val tracks = player.currentTracks
    var textGroupIndex = -1
    var textTrackIndex = 0
    var count = 0
    for (gi in tracks.groups.indices) {
        val group = tracks.groups[gi]
        if (group.type == C.TRACK_TYPE_TEXT && group.isSupported) {
            for (ti in 0 until group.length) {
                if (count == index) {
                    textGroupIndex = gi
                    textTrackIndex = ti
                }
                count++
            }
        }
    }
    if (textGroupIndex >= 0) {
        val group = tracks.groups[textGroupIndex]
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .addOverride(
                androidx.media3.common.TrackSelectionOverride(
                    group.mediaTrackGroup, listOf(textTrackIndex)
                )
            )
            .build()
    }
}

// === PiP 自绘图标（无系统媒体描边）===

private enum class PipIcon { PLAY, PAUSE, PREV, NEXT }

/**
 * 在透明背景上自绘 PiP 操作图标，避免使用系统 media drawable 自带的外圈/描边。
 */
private fun createPipIcon(context: Context, type: PipIcon): Icon {
    val size = 64
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply {
        color = AndroidColor.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val s = size.toFloat()
    when (type) {
        PipIcon.PLAY -> {
            val p = Path().apply {
                moveTo(s * 0.36f, s * 0.26f)
                lineTo(s * 0.36f, s * 0.74f)
                lineTo(s * 0.74f, s * 0.5f)
                close()
            }
            canvas.drawPath(p, paint)
        }
        PipIcon.PAUSE -> {
            canvas.drawRect(s * 0.34f, s * 0.26f, s * 0.45f, s * 0.74f, paint)
            canvas.drawRect(s * 0.55f, s * 0.26f, s * 0.66f, s * 0.74f, paint)
        }
        PipIcon.PREV -> {
            canvas.drawRect(s * 0.60f, s * 0.26f, s * 0.68f, s * 0.74f, paint)
            val p = Path().apply {
                moveTo(s * 0.58f, s * 0.26f)
                lineTo(s * 0.58f, s * 0.74f)
                lineTo(s * 0.24f, s * 0.5f)
                close()
            }
            canvas.drawPath(p, paint)
        }
        PipIcon.NEXT -> {
            canvas.drawRect(s * 0.32f, s * 0.26f, s * 0.40f, s * 0.74f, paint)
            val p = Path().apply {
                moveTo(s * 0.42f, s * 0.26f)
                lineTo(s * 0.42f, s * 0.74f)
                lineTo(s * 0.76f, s * 0.5f)
                close()
            }
            canvas.drawPath(p, paint)
        }
    }
    return Icon.createWithBitmap(bmp)
}
