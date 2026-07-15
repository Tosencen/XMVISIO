package com.xmvisio.app.ui.player

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import com.xmvisio.app.data.VideoInfo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.xmvisio.app.data.VideoContentScale

enum class OverlayPanelType {
    NONE, AUDIO_TRACK, SUBTITLE, PLAYBACK_SPEED, VIDEO_SCALE, PLAYLIST
}

@Composable
fun VideoPlayerOverlayPanel(
    panelType: OverlayPanelType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (panelType == OverlayPanelType.NONE) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .align(Alignment.CenterEnd)
                .clickable { /* consume click */ }
                .background(
                    Color.Black.copy(alpha = 0.85f),
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}

// === 音轨面板（真实轨道） ===
@Composable
fun AudioTrackPanel(
    player: Player,
    selectedAudioTrackIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val tracksState = rememberAudioTracks(player)

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "音频轨道", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // "关闭" 选项
            item {
                PanelRadioItem(
                    title = "关闭",
                    selected = selectedAudioTrackIndex == -1,
                    onClick = { onTrackSelected(-1) }
                )
            }
            items(tracksState.size) { index ->
                val info = tracksState[index]
                PanelRadioItem(
                    title = info.label,
                    selected = index == selectedAudioTrackIndex,
                    onClick = { onTrackSelected(index) }
                )
            }
        }
    }
}

// === 字幕面板（真实轨道） ===
@Composable
fun SubtitlePanel(
    player: Player,
    selectedSubtitleIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val tracksState = rememberTextTracks(player)

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "字幕", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // "关闭" 选项
            item {
                PanelRadioItem(
                    title = "关闭",
                    selected = selectedSubtitleIndex == -1,
                    onClick = { onTrackSelected(-1) }
                )
            }
            items(tracksState.size) { index ->
                val info = tracksState[index]
                PanelRadioItem(
                    title = info.label,
                    selected = index == selectedSubtitleIndex,
                    onClick = { onTrackSelected(index) }
                )
            }
        }
    }
}

// === 轨道状态 ===

data class TrackInfo(val label: String, val trackGroupIndex: Int, val trackIndex: Int)

@Composable
fun rememberAudioTracks(player: Player): List<TrackInfo> {
    var tracks by remember { mutableStateOf(extractAudioTracks(player)) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(trackGroups: Tracks) {
                tracks = extractAudioTracksFromTracks(trackGroups)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    return tracks
}

@Composable
fun rememberTextTracks(player: Player): List<TrackInfo> {
    var tracks by remember { mutableStateOf(extractTextTracks(player)) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(trackGroups: Tracks) {
                tracks = extractTextTracksFromTracks(trackGroups)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    return tracks
}

private fun extractAudioTracks(player: Player): List<TrackInfo> =
    extractAudioTracksFromTracks(player.currentTracks)

private fun extractTextTracks(player: Player): List<TrackInfo> =
    extractTextTracksFromTracks(player.currentTracks)

private fun extractAudioTracksFromTracks(tracks: Tracks): List<TrackInfo> {
    val result = mutableListOf<TrackInfo>()
    for (groupIndex in tracks.groups.indices) {
        val group = tracks.groups[groupIndex]
        if (group.type == C.TRACK_TYPE_AUDIO && group.isSupported) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val lang = format.language?.let { "[${it.uppercase()}] " } ?: ""
                val label = format.label ?: "音轨 ${result.size + 1}"
                result.add(TrackInfo(label = "$lang$label", trackGroupIndex = groupIndex, trackIndex = trackIndex))
            }
        }
    }
    return result
}

private fun extractTextTracksFromTracks(tracks: Tracks): List<TrackInfo> {
    val result = mutableListOf<TrackInfo>()
    for (groupIndex in tracks.groups.indices) {
        val group = tracks.groups[groupIndex]
        if (group.type == C.TRACK_TYPE_TEXT && group.isSupported) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val lang = format.language?.let { "[${it.uppercase()}] " } ?: ""
                val label = format.label ?: "字幕 ${result.size + 1}"
                result.add(TrackInfo(label = "$lang$label", trackGroupIndex = groupIndex, trackIndex = trackIndex))
            }
        }
    }
    return result
}

@Composable
fun PlaybackSpeedPanel(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onBack: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "播放速度", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(speeds) { speed ->
                PanelRadioItem(
                    title = "${speed}x",
                    selected = speed == currentSpeed,
                    onClick = { onSpeedSelected(speed) }
                )
            }
        }
    }
}

@Composable
fun VideoScalePanel(
    currentScale: VideoContentScale,
    onScaleSelected: (VideoContentScale) -> Unit,
    onBack: () -> Unit
) {
    val scales = listOf(
        VideoContentScale.BEST_FIT to "最佳适配",
        VideoContentScale.STRETCH to "拉伸",
        VideoContentScale.CROP to "裁剪",
        VideoContentScale.HUNDRED_PERCENT to "100%"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "视频缩放", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(scales) { (scale, label) ->
                PanelRadioItem(
                    title = label,
                    selected = scale == currentScale,
                    onClick = { onScaleSelected(scale) }
                )
            }
        }
    }
}

@Composable
fun PlaylistPanel(
    videos: List<VideoInfo>,
    currentIndex: Int,
    onVideoSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "播放列表", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(videos) { index, video ->
                val isCurrent = index == currentIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isCurrent) onVideoSelected(index)
                        },
                    color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = video.name,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = video.formattedDuration,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// === 辅助组件 ===

@Composable
private fun PanelHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PanelRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = Color.White.copy(alpha = 0.5f)
                )
            )
            Text(
                text = title,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
