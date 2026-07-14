package com.xmvisio.app.ui.player

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.xmvisio.app.data.VideoContentScale

enum class OverlayPanelType {
    NONE, AUDIO_TRACK, SUBTITLE, PLAYBACK_SPEED, VIDEO_SCALE
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

@Composable
fun AudioTrackPanel(
    currentTrack: String,
    onTrackSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val tracks = listOf("默认", "音轨 1", "音轨 2", "关闭")

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "音频轨道", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tracks) { track ->
                PanelRadioItem(
                    title = track,
                    selected = track == currentTrack,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
}

@Composable
fun SubtitlePanel(
    currentTrack: String,
    onTrackSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val tracks = listOf("关闭", "字幕 1", "字幕 2", "外部字幕...")

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(title = "字幕", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tracks) { track ->
                PanelRadioItem(
                    title = track,
                    selected = track == currentTrack,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
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
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
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
