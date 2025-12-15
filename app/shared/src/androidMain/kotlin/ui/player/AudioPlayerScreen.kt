package com.xmvisio.app.ui.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.AudioPlayer
import com.xmvisio.app.audio.GlobalAudioPlayer
import com.xmvisio.app.audio.LocalAudioFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 音频播放器界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerScreen(
    audio: LocalAudioFile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 使用全局单例播放器实例
    val audioPlayer: AudioPlayer = remember { GlobalAudioPlayer.getInstance(context) }
    
    // 播放状态
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPosition by audioPlayer.currentPosition.collectAsState()
    val duration by audioPlayer.duration.collectAsState()
    val playbackSpeed by audioPlayer.playbackSpeed.collectAsState()
    
    // 获取跳过时间设置
    val prefs = remember { context.getSharedPreferences("audio_settings", android.content.Context.MODE_PRIVATE) }
    val seekTimeInSeconds = remember { prefs.getInt("seek_time", 10) }
    
    // 对话框状态
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 睡眠定时器状态
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var sleepTimerRemaining by remember { mutableStateOf<Duration?>(null) }
    
    // 准备播放器
    LaunchedEffect(audio.id) {
        audioPlayer.prepare(
            uri = audio.uri,
            audioId = audio.id,
            onPrepared = {
                // 准备完成后可以自动播放
                audioPlayer.play()
            },
            onError = { error ->
                // TODO: 显示错误提示
                println("播放器错误: ${error.message}")
            }
        )
    }
    
    // 更新播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100) // 每100ms更新一次
            audioPlayer.updateCurrentPosition()
        }
    }
    
    // 睡眠定时器倒计时
    LaunchedEffect(sleepTimerEndTime) {
        sleepTimerEndTime?.let { endTime ->
            while (System.currentTimeMillis() < endTime) {
                val remaining = (endTime - System.currentTimeMillis()).milliseconds
                sleepTimerRemaining = remaining
                delay(1000) // 每秒更新一次
            }
            // 时间到，暂停播放
            audioPlayer.pause()
            sleepTimerEndTime = null
            sleepTimerRemaining = null
        }
    }
    
    // 返回键处理（不暂停播放，让音频在后台继续）
    BackHandler {
        onClose()
    }
    
    // 清理资源（注释掉，让播放器在后台继续运行）
    // DisposableEffect(Unit) {
    //     onDispose {
    //         audioPlayer.release()
    //     }
    // }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = audio.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        if (audio.artist != null) {
                            Text(
                                text = audio.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 菜单按钮
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "菜单")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("播放速度") },
                                onClick = {
                                    showMenu = false
                                    showSpeedDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, null)
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("睡眠定时器") },
                                onClick = {
                                    showMenu = false
                                    showSleepTimerDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Timer, null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 睡眠定时器指示器
            sleepTimerRemaining?.let { remaining ->
                SleepTimerIndicator(
                    remainingTime = remaining,
                    onCancel = {
                        sleepTimerEndTime = null
                        sleepTimerRemaining = null
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // 封面占位
            Surface(
                modifier = Modifier
                    .size(280.dp)
                    .padding(bottom = 48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 进度条
            ProgressSlider(
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { position ->
                    audioPlayer.seekTo(position)
                }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 播放控制按钮
            PlaybackControls(
                isPlaying = isPlaying,
                onPlayPauseClick = {
                    audioPlayer.togglePlayPause()
                },
                onRewindClick = {
                    audioPlayer.rewind(seekTimeInSeconds)
                },
                onFastForwardClick = {
                    audioPlayer.fastForward(seekTimeInSeconds)
                }
            )
        }
    }
    
    // 播放速度对话框
    if (showSpeedDialog) {
        SpeedDialog(
            currentSpeed = playbackSpeed,
            onSpeedChange = { speed ->
                audioPlayer.setPlaybackSpeed(speed)
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
    
    // 睡眠定时器对话框
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onSetTimer = { duration ->
                sleepTimerEndTime = System.currentTimeMillis() + duration.inWholeMilliseconds
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

/**
 * 进度条组件
 */
@Composable
private fun ProgressSlider(
    currentPosition: Duration,
    duration: Duration,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 进度条
        Slider(
            value = if (duration.inWholeMilliseconds > 0) {
                (currentPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat())
            } else {
                0f
            },
            onValueChange = { value ->
                val newPosition = (duration.inWholeMilliseconds * value).toLong()
                onSeek(newPosition.milliseconds)
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 播放控制按钮组件
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onRewindClick: () -> Unit,
    onFastForwardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 快退按钮
        IconButton(
            onClick = onRewindClick,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.Replay10,
                contentDescription = "快退",
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // 播放/暂停按钮
        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(80.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // 快进按钮
        IconButton(
            onClick = onFastForwardClick,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.Forward10,
                contentDescription = "快进",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 格式化时长显示
 */
private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
