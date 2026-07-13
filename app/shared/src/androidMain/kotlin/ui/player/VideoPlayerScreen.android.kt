package com.xmvisio.app.ui.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import com.xmvisio.app.data.VideoInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayerScreen(
    video: VideoInfo,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember(player) {
        mutableStateOf(player.isPlaying)
    }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(player) {
        snapshotFlow { player.isPlaying }.collect { isPlaying = it }
    }

    LaunchedEffect(player) {
        while (isActive) {
            currentPosition = player.currentPosition
            totalDuration = player.duration
            delay(200)
        }
    }

    BackHandler {
        if (showControls) {
            onClose()
        } else {
            showControls = true
        }
    }

    val screenBrightness = remember { mutableStateOf(-1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize(),
        )

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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = video.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        PlayerActionButton(Icons.Default.ClosedCaption, "字幕") { }
                        PlayerActionButton(Icons.Default.Tune, "音轨") { }
                        Box {
                            PlayerActionButton(Icons.Default.Speed, "速度") { showSpeedMenu = true }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${speed}x",
                                                color = if (speed == playbackSpeed) MaterialTheme.colorScheme.primary else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            playbackSpeed = speed
                                            player.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        PlayerActionButton(Icons.Default.PlaylistPlay, "播放列表") { }
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
                        onClick = { player.seekToPreviousMediaItem() }
                    )
                    IconButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                    PlayerActionButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "下一个",
                        size = 48,
                        onClick = { player.seekToNextMediaItem() }
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            value = if (totalDuration > 0) {
                                (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { fraction ->
                                player.seekTo((fraction * totalDuration).toLong())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
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
                        PlayerActionButton(Icons.Outlined.Lock, "锁定") { }
                        PlayerActionButton(Icons.Default.AspectRatio, "缩放") { }
                        PlayerActionButton(Icons.Default.PictureInPicture, "画中画") { }
                        PlayerActionButton(Icons.Default.Repeat, "循环") { }
                        PlayerActionButton(Icons.Default.Shuffle, "随机") { }
                        PlayerActionButton(Icons.Default.ScreenRotation, "旋转") { }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int = 36,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.dp),
            tint = Color.White
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
