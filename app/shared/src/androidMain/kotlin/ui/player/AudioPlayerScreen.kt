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
    
    // 当前播放的音频（可变状态，用于切换音频时更新UI）
    var currentAudio by remember { mutableStateOf(audio) }
    
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
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    // 获取当前分类的播放列表
    val categoryManager = remember { com.xmvisio.app.audio.CategoryManager(context) }
    val audioScanner = remember { com.xmvisio.app.audio.AudioScanner(context) }
    var playlist by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    
    LaunchedEffect(currentAudio.id) {
        // 获取当前音频所属的分类
        val categoryId = categoryManager.getAudioCategory(currentAudio.id)
        val allAudios = audioScanner.scanAudioFiles()
        
        playlist = if (categoryId != null) {
            // 获取该分类下的所有音频
            val audioIds = categoryManager.getAudioIdsByCategory(categoryId)
            allAudios.filter { it.id in audioIds }
        } else {
            // 如果没有分类，显示所有音频
            allAudios
        }
    }
    
    // 睡眠定时器状态
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var sleepTimerRemaining by remember { mutableStateOf<Duration?>(null) }
    
    // 准备播放器
    LaunchedEffect(currentAudio.id) {
        audioPlayer.prepare(
            uri = currentAudio.uri,
            audioId = currentAudio.id,
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
                            text = currentAudio.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        currentAudio.artist?.let { artist ->
                            Text(
                                text = artist,
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
            // 封面占位（点击显示播放列表）
            Surface(
                onClick = { showPlaylistDialog = true },
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
            
            // 进度条（带播放速度和睡眠定时器按钮）
            ProgressSlider(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                sleepTimerRemaining = sleepTimerRemaining,
                onSeek = { position ->
                    audioPlayer.seekTo(position)
                },
                onSpeedClick = { showSpeedDialog = true },
                onTimerClick = { showSleepTimerDialog = true }
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
    
    // 播放列表对话框
    if (showPlaylistDialog) {
        PlaylistDialog(
            playlist = playlist,
            currentAudioId = currentAudio.id,
            isPlaying = isPlaying,
            onAudioClick = { selectedAudio: LocalAudioFile ->
                scope.launch {
                    currentAudio = selectedAudio
                    audioPlayer.prepare(
                        uri = selectedAudio.uri,
                        audioId = selectedAudio.id,
                        onPrepared = {
                            audioPlayer.play()
                        },
                        onError = { error ->
                            println("播放器错误: ${error.message}")
                        }
                    )
                }
            },
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

/**
 * 进度条组件（带播放速度和睡眠定时器按钮）
 */
@Composable
private fun ProgressSlider(
    currentPosition: Duration,
    duration: Duration,
    playbackSpeed: Float,
    sleepTimerRemaining: Duration?,
    onSeek: (Duration) -> Unit,
    onSpeedClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 播放速度和睡眠定时器按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放速度按钮
            FilledTonalButton(
                onClick = onSpeedClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 睡眠定时器按钮
            FilledTonalButton(
                onClick = onTimerClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sleepTimerRemaining != null) {
                        formatSleepTimerShort(sleepTimerRemaining)
                    } else {
                        "定时"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
 * 格式化睡眠定时器显示（精确到秒）
 */
private fun formatSleepTimerShort(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
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
