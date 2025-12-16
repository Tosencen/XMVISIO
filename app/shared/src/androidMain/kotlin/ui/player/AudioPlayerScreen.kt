package com.xmvisio.app.ui.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val coroutineScope = rememberCoroutineScope()
    
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
    var categoryName by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentAudio.id) {
        // 获取当前音频所属的分类
        val categoryId = categoryManager.getAudioCategory(currentAudio.id)
        val allAudios = audioScanner.scanAudioFiles()
        
        // 获取分类名称
        categoryName = if (categoryId != null) {
            val categories = categoryManager.getCategories()
            categories.find { it.id == categoryId }?.name
        } else {
            null
        }
        
        val baselist = if (categoryId != null) {
            // 获取该分类下的所有音频
            val audioIds = categoryManager.getAudioIdsByCategory(categoryId)
            allAudios.filter { it.id in audioIds }
        } else {
            // 如果没有分类，显示所有音频
            allAudios
        }
        
        // 应用自定义排序（与首页保持一致）
        val audioOrderManager = com.xmvisio.app.audio.AudioOrderManager(context)
        val customOrder = audioOrderManager.getOrderSync(categoryId)
        
        playlist = if (customOrder != null) {
            // 按照自定义排序重新排列
            val orderedList = mutableListOf<LocalAudioFile>()
            customOrder.forEach { audioId ->
                baselist.find { it.id == audioId }?.let { orderedList.add(it) }
            }
            // 添加不在自定义排序中的新音频
            baselist.forEach { audio ->
                if (audio.id !in customOrder) {
                    orderedList.add(audio)
                }
            }
            orderedList
        } else {
            baselist
        }
        
        // 设置播放列表到播放器，用于自动播放下一首
        audioPlayer.setPlaylist(
            uris = playlist.map { it.uri },
            ids = playlist.map { it.id },
            onPlayNext = { nextId ->
                // 找到下一首音频并播放
                val nextAudio = playlist.find { it.id == nextId }
                if (nextAudio != null) {
                    currentAudio = nextAudio
                    // 准备并播放下一首
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        audioPlayer.prepare(
                            uri = nextAudio.uri,
                            audioId = nextAudio.id,
                            onPrepared = {
                                audioPlayer.play()
                            },
                            onError = { error ->
                                println("播放器错误: ${error.message}")
                            }
                        )
                    }
                }
            }
        )
    }
    
    // 使用全局睡眠定时器
    val sleepTimerManager = remember { com.xmvisio.app.audio.SleepTimerManager.getInstance(context) }
    val sleepTimerRemaining by sleepTimerManager.remainingTime.collectAsState()
    
    // 使用全局播放器控制器（管理通知）
    val playerController = remember { com.xmvisio.app.audio.GlobalAudioPlayerController.getInstance(context) }
    
    // 更新播放器控制器的当前音频和播放列表
    LaunchedEffect(currentAudio.id, playlist) {
        playerController.setCurrentAudio(currentAudio, playlist)
    }
    val isSetToAudioEnd by sleepTimerManager.isSetToAudioEnd.collectAsState()
    
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
    
    // 睡眠定时器由全局管理器处理，不需要在这里处理
    
    // 下滑手势状态 - 使用 Animatable 实现平滑动画
    val dragOffsetAnimatable = remember { androidx.compose.animation.core.Animatable(0f) }
    val dragThreshold = 200f // 下滑超过这个距离就关闭
    
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
    
    // 计算透明度：拖动时逐渐变透明
    val alpha = remember {
        derivedStateOf {
            val progress = (dragOffsetAnimatable.value / 1000f).coerceIn(0f, 1f)
            1f - progress
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { androidx.compose.ui.unit.IntOffset(0, dragOffsetAnimatable.value.toInt()) }
            .graphicsLayer {
                // 透明度动画
                this.alpha = alpha.value
            }
            .pointerInput(Unit) {
                val velocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                
                detectVerticalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().y
                        val currentOffset = dragOffsetAnimatable.value
                        
                        // 根据速度和偏移量决定是否关闭
                        if (velocity > 1000 || currentOffset > dragThreshold) {
                            // 快速向下滑动或超过阈值，关闭播放器
                            coroutineScope.launch {
                                dragOffsetAnimatable.animateTo(
                                    targetValue = 2000f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    )
                                )
                                onClose()
                            }
                        } else {
                            // 平滑回到原位（无回弹）
                            coroutineScope.launch {
                                dragOffsetAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 300,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        // 取消拖动，平滑回到原位（无回弹）
                        coroutineScope.launch {
                            dragOffsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 300,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            )
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        velocityTracker.addPointerInputChange(change)
                        // 只允许向下拖动
                        if (dragAmount > 0 || dragOffsetAnimatable.value > 0) {
                            coroutineScope.launch {
                                val newValue = (dragOffsetAnimatable.value + dragAmount).coerceAtLeast(0f)
                                dragOffsetAnimatable.snapTo(newValue)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                // 添加缩放效果，拖动时略微缩小
                val scale = 1f - (dragOffsetAnimatable.value / 2000f).coerceIn(0f, 0.05f)
                scaleX = scale
                scaleY = scale
                
                // 添加圆角效果
                val cornerRadius = (dragOffsetAnimatable.value / 10f).coerceIn(0f, 32f)
                clip = true
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = cornerRadius.dp,
                    topEnd = cornerRadius.dp
                )
            }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 顶部分类名称
        categoryName?.let { name ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = if (categoryName != null) 80.dp else 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 封面占位（点击显示播放列表）
            Surface(
                onClick = { showPlaylistDialog = true },
                modifier = Modifier
                    .size(280.dp),
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
            
            // 歌曲标题和艺术家（居中显示）
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentAudio.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                
                currentAudio.artist?.let { artist ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 进度条（带播放速度和睡眠定时器按钮）
            ProgressSlider(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                sleepTimerRemaining = sleepTimerRemaining,
                isSetToAudioEnd = isSetToAudioEnd,
                onSeek = { position ->
                    audioPlayer.seekTo(position)
                },
                onSpeedClick = { showSpeedDialog = true },
                onTimerClick = { showSleepTimerDialog = true },
                onTimerCancel = { sleepTimerManager.cancelTimer() }
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
                sleepTimerManager.setTimer(duration)
            },
            onSetTimerAtAudioEnd = {
                sleepTimerManager.setTimerAtAudioEnd()
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
                coroutineScope.launch {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSlider(
    currentPosition: Duration,
    duration: Duration,
    playbackSpeed: Float,
    sleepTimerRemaining: Duration?,
    isSetToAudioEnd: Boolean,
    onSeek: (Duration) -> Unit,
    onSpeedClick: () -> Unit,
    onTimerClick: () -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sliderStyleManager = remember { com.xmvisio.app.data.SliderStyleManager.getInstance(context) }
    val sliderStyle by sliderStyleManager.sliderStyle.collectAsState()
    
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
            
            // 睡眠定时器按钮（点击设置，长按取消）
            FilledTonalButton(
                onClick = {
                    if (sleepTimerRemaining != null || isSetToAudioEnd) {
                        // 如果已有定时器，点击取消
                        onTimerCancel()
                    } else {
                        // 否则打开设置对话框
                        onTimerClick()
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        sleepTimerRemaining != null -> formatSleepTimerShort(sleepTimerRemaining)
                        isSetToAudioEnd -> "音频结束"
                        else -> "定时"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 根据样式渲染不同的进度条
        when (sliderStyle) {
            com.xmvisio.app.data.SliderStyle.DEFAULT -> {
                // 默认样式：标准 Slider
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
            }
            
            com.xmvisio.app.data.SliderStyle.SQUIGGLY -> {
                // 波浪样式：暂时使用默认样式（需要第三方库支持）
                // TODO: 集成 SquigglySlider 库
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
            }
            
            com.xmvisio.app.data.SliderStyle.SLIM -> {
                // 纤细样式：无滑块的自定义轨道
                val sliderState = remember {
                    androidx.compose.material3.SliderState(
                        value = 0f,
                        valueRange = 0f..1f
                    )
                }
                
                // 更新 slider 状态
                LaunchedEffect(currentPosition, duration) {
                    if (duration.inWholeMilliseconds > 0) {
                        sliderState.value = (currentPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat())
                    }
                }
                
                Slider(
                    state = sliderState,
                    onValueChange = { value ->
                        val newPosition = (duration.inWholeMilliseconds * value).toLong()
                        onSeek(newPosition.milliseconds)
                    },
                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                    track = { state ->
                        com.xmvisio.app.ui.foundation.PlayerSliderTrack(
                            sliderState = state,
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                activeTickColor = MaterialTheme.colorScheme.primary,
                                inactiveTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            trackHeight = 10.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
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
