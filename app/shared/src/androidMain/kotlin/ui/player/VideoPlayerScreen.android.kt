package com.xmvisio.app.ui.player

import android.media.AudioManager
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.xmvisio.app.data.DoubleTapGesture
import com.xmvisio.app.data.LoopMode
import com.xmvisio.app.data.ScreenOrientation
import com.xmvisio.app.data.VideoContentScale
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.data.VideoPlayerPreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayerScreen(
    video: VideoInfo,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { VideoPlayerPreferencesManager.getInstance(context) }
    val prefs by prefsManager.preferences.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
            prepare()
            playWhenReady = prefs.autoplay
            setPlaybackSpeed(prefs.defaultPlaybackSpeed)
            repeatMode = when (prefs.loopMode) {
                LoopMode.OFF -> Player.REPEAT_MODE_OFF
                LoopMode.ONE -> Player.REPEAT_MODE_ONE
                LoopMode.ALL -> Player.REPEAT_MODE_ALL
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // === 播放状态 ===
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(prefs.defaultPlaybackSpeed) }
    var loopMode by remember { mutableStateOf(prefs.loopMode) }
    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
        })
    }

    LaunchedEffect(player) {
        while (isActive) {
            currentPosition = player.currentPosition
            totalDuration = player.duration
            delay(200)
        }
    }

    // === 手势状态 ===
    var seekIndicator by remember { mutableStateOf(0L) } // seek 量（毫秒），正=前进，负=后退
    var showSeekIndicator by remember { mutableStateOf(false) }
    var volumeIndicator by remember { mutableFloatStateOf(-1f) } // -1 = 不显示
    var brightnessIndicator by remember { mutableFloatStateOf(-1f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressSpeedDisplay by remember { mutableFloatStateOf(2f) }

    // 双击状态
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }
    var lastTapY by remember { mutableFloatStateOf(0f) }
    var doubleTapSide by remember { mutableStateOf(0) } // -1=左, 0=中, 1=右
    var showDoubleTapIndicator by remember { mutableStateOf(false) }

    // 侧滑面板状态
    var activeOverlayPanel by remember { mutableStateOf(OverlayPanelType.NONE) }
    var selectedAudioTrack by remember { mutableStateOf("默认") }
    var selectedSubtitle by remember { mutableStateOf("关闭") }
    var selectedVideoScale by remember { mutableStateOf(prefs.videoContentScale) }
    var showPlaylist by remember { mutableStateOf(false) }

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
        }
    }

    // Seek 指示器自动隐藏
    LaunchedEffect(showSeekIndicator) {
        if (showSeekIndicator) {
            delay(600)
            showSeekIndicator = false
        }
    }

    // 音量控制
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // 亮度控制
    val window = (context as? android.app.Activity)?.window
    val layoutParams = window?.attributes
    val activity = context as? android.app.Activity

    // 屏幕旋转状态
    var rotationIndex by remember { mutableIntStateOf(0) }
    val rotationValues = listOf(
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR,
    )
    val rotationLabels = listOf("自动", "横屏", "反向横屏", "竖屏", "跟随传感器")

    // 进入播放页面时设置屏幕方向，退出时恢复
    DisposableEffect(Unit) {
        val act = activity
        val originalOrientation = act?.requestedOrientation
        val orientation = when (prefs.screenOrientation) {
            ScreenOrientation.AUTOMATIC -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.LANDSCAPE_REVERSE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            ScreenOrientation.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.VIDEO_ORIENTATION -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        act?.requestedOrientation = orientation
        onDispose {
            act?.requestedOrientation = originalOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (prefs.controlsLocked) return@BackHandler
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
                .pointerInput(prefs.enableSeekGesture, prefs.enableVolumeGesture, prefs.enableBrightnessGesture, prefs.controlsLocked) {
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
                            val now = System.currentTimeMillis()
                            val centerX = size.width / 2f
                            val diff = now - lastTapTime

                            if (diff < 300 && abs(offset.y - lastTapY) < 100) {
                                // 双击检测
                                when {
                                    prefs.doubleTapGesture == DoubleTapGesture.PLAY_PAUSE ||
                                    (prefs.doubleTapGesture == DoubleTapGesture.BOTH && abs(offset.x - centerX) < size.width * 0.2f) -> {
                                        if (player.isPlaying) player.pause() else player.play()
                                    }
                                    else -> {
                                        val seekMs = prefs.seekIncrement * 1000L
                                        if (offset.x < centerX) {
                                            // 左侧：后退
                                            val newPos = (player.currentPosition - seekMs).coerceAtLeast(0)
                                            player.seekTo(newPos)
                                            seekIndicator = -seekMs
                                            doubleTapSide = -1
                                        } else {
                                            // 右侧：前进
                                            val newPos = (player.currentPosition + seekMs).coerceAtMost(player.duration)
                                            player.seekTo(newPos)
                                            seekIndicator = seekMs
                                            doubleTapSide = 1
                                        }
                                        showDoubleTapIndicator = true
                                    }
                                }
                                lastTapTime = 0
                            } else {
                                lastTapTime = now
                                lastTapX = offset.x
                                lastTapY = offset.y
                            }
                        },
                        onLongPress = {
                            if (prefs.controlsLocked || !prefs.enableLongPress) return@detectTapGestures
                            isLongPressing = true
                            longPressSpeedDisplay = prefs.longPressSpeed
                            player.setPlaybackSpeed(prefs.longPressSpeed)
                        }
                    )
                }
                .pointerInput(prefs.enableSeekGesture, prefs.enableVolumeGesture, prefs.enableBrightnessGesture, prefs.controlsLocked) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val thirdWidth = size.width / 3f
                            // 记录起始区域
                        },
                        onDragEnd = {
                            if (isLongPressing) {
                                isLongPressing = false
                                player.setPlaybackSpeed(playbackSpeed)
                            }
                            volumeIndicator = -1f
                            brightnessIndicator = -1f
                        },
                        onDragCancel = {
                            if (isLongPressing) {
                                isLongPressing = false
                                player.setPlaybackSpeed(playbackSpeed)
                            }
                            volumeIndicator = -1f
                            brightnessIndicator = -1f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            if (isLongPressing || prefs.controlsLocked) return@detectDragGestures

                            val thirdWidth = size.width / 3f
                            val x = change.position.x

                            when {
                                // 左侧垂直滑动 = 亮度
                                x < thirdWidth && prefs.enableBrightnessGesture -> {
                                    val sensitivity = prefs.brightnessSensitivity
                                    val delta = -dragAmount.y * sensitivity * 0.01f
                                    val currentBrightness = if (layoutParams != null) {
                                        layoutParams.screenBrightness
                                    } else {
                                        0.5f
                                    }
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                                    if (layoutParams != null) {
                                        layoutParams.screenBrightness = newBrightness
                                        window.attributes = layoutParams
                                    }
                                    brightnessIndicator = newBrightness
                                }
                                // 右侧垂直滑动 = 音量
                                x > size.width - thirdWidth && prefs.enableVolumeGesture -> {
                                    val sensitivity = prefs.volumeSensitivity
                                    val delta = -dragAmount.y * sensitivity * 0.5f
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val newVol = (currentVol + delta.roundToInt()).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    volumeIndicator = newVol.toFloat() / maxVolume
                                }
                                // 水平滑动 = seek
                                abs(dragAmount.x) > abs(dragAmount.y) && prefs.enableSeekGesture -> {
                                    val sensitivity = prefs.seekSensitivity
                                    val seekAmount = dragAmount.x * sensitivity * 50f
                                    val delta = seekAmount.toLong()
                                    val newPos = (player.currentPosition + delta).coerceIn(0, player.duration)
                                    seekIndicator = delta
                                    player.seekTo(newPos)
                                    showSeekIndicator = true
                                }
                            }
                        }
                    )
                }
        ) {
            val videoState = rememberPresentationState(player)
            val contentScale = when (selectedVideoScale) {
                VideoContentScale.BEST_FIT -> androidx.compose.ui.layout.ContentScale.Fit
                VideoContentScale.STRETCH -> androidx.compose.ui.layout.ContentScale.FillBounds
                VideoContentScale.CROP -> androidx.compose.ui.layout.ContentScale.Crop
                VideoContentScale.HUNDRED_PERCENT -> androidx.compose.ui.layout.ContentScale.None
            }
            PlayerSurface(
                player = player,
                modifier = Modifier
                    .fillMaxSize()
                    .resizeWithContentScale(
                        contentScale = contentScale,
                        sourceSizeDp = videoState.videoSizeDp
                    ),
            )
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
                contentAlignment = if (doubleTapSide < 0) Alignment.CenterStart
                    else if (doubleTapSide > 0) Alignment.CenterEnd
                    else Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (doubleTapSide > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // === 音量指示器 ===
        if (volumeIndicator >= 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = volumeIndicator)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Text(
                        text = "${(volumeIndicator * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // === 亮度指示器 ===
        if (brightnessIndicator >= 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = brightnessIndicator)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Text(
                        text = "${(brightnessIndicator * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
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
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Text(
                        text = "长按快进 ${longPressSpeedDisplay}x",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // === 锁定状态：显示解锁按钮 ===
        if (prefs.controlsLocked && showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { prefsManager.setControlsLocked(false) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "解锁",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text("轻触解锁", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // === 控件层 ===
        AnimatedVisibility(
            visible = showControls,
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
                            text = video.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
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
                        PlayerActionButton(Icons.Default.PlaylistPlay, "播放列表") { if (!prefs.controlsLocked) showPlaylist = true }
                    }
                }

                // 中间播放控制
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    PlayerActionButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "上一个",
                        size = 48,
                        onClick = { if (!prefs.controlsLocked) { val p = player.currentPosition - 10000; player.seekTo(p.coerceAtLeast(0)) } }
                    )
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
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                    PlayerActionButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "下一个",
                        size = 48,
                        onClick = { if (!prefs.controlsLocked) { val p = player.currentPosition + 10000; player.seekTo(p.coerceAtMost(player.duration)) } }
                    )
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
                        Slider(
                            value = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f,
                            onValueChange = { fraction ->
                                if (!prefs.controlsLocked) player.seekTo((fraction * totalDuration).toLong())
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
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
                        // 锁定控件
                        PlayerActionButton(
                            icon = if (prefs.controlsLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                            contentDescription = if (prefs.controlsLocked) "解锁" else "锁定",
                            onClick = { prefsManager.setControlsLocked(!prefs.controlsLocked) }
                        )
                        PlayerActionButton(Icons.Default.AspectRatio, "缩放", onClick = {
                            if (!prefs.controlsLocked) activeOverlayPanel = if (activeOverlayPanel == OverlayPanelType.VIDEO_SCALE) OverlayPanelType.NONE else OverlayPanelType.VIDEO_SCALE
                        })
                        PlayerActionButton(Icons.Default.PictureInPicture, "画中画", onClick = {
                            if (!prefs.controlsLocked && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                } catch (_: Exception) { }
                            }
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
                        PlayerActionButton(Icons.Default.Shuffle, "随机", onClick = { })
                        PlayerActionButton(
                            icon = Icons.Default.ScreenRotation,
                            contentDescription = "旋转：${rotationLabels[rotationIndex]}",
                            onClick = {
                                if (!prefs.controlsLocked) {
                                    rotationIndex = (rotationIndex + 1) % rotationValues.size
                                    activity?.requestedOrientation = rotationValues[rotationIndex]
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
                    currentTrack = selectedAudioTrack,
                    onTrackSelected = {
                        selectedAudioTrack = it
                        player.volume = if (it == "关闭") 0f else 1f
                        activeOverlayPanel = OverlayPanelType.NONE
                    },
                    onBack = { activeOverlayPanel = OverlayPanelType.NONE }
                )
                OverlayPanelType.SUBTITLE -> SubtitlePanel(
                    currentTrack = selectedSubtitle,
                    onTrackSelected = {
                        selectedSubtitle = it
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setPreferredTextLanguage(it.takeIf { s -> s != "关闭" && s != "关闭" && s != "外部字幕..." })
                            .build()
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
                else -> {}
            }
        }
    }

    // === 播放列表弹窗 ===
    if (showPlaylist) {
        AlertDialog(
            onDismissRequest = { showPlaylist = false },
            title = { Text("播放列表") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(video.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("正在播放", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylist = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int = 40,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
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
