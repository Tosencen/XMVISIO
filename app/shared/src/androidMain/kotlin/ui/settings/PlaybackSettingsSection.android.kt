package com.xmvisio.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.PlaybackSettingsManager
import com.xmvisio.app.data.SliderStyleManager

@Composable
actual fun PlaybackSettingsSection(
    onNavigateToTheme: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { PlaybackSettingsManager.getInstance(context) }
    val skipSilence by settingsManager.skipSilence.collectAsState()
    val volumeBoost by settingsManager.volumeBoost.collectAsState()
    var showVolumeBoostDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }

    val sliderStyleManager = remember { SliderStyleManager.getInstance(context) }
    val currentStyle by sliderStyleManager.sliderStyle.collectAsState()

    // 主题与色彩
    SettingsCardItem(
        icon = Icons.Filled.Palette,
        title = "主题与色彩",
        subtitle = "外观模式、主题色",
        onClick = onNavigateToTheme,
        showChevron = true,
        isFirst = true
    )

    // 跳过空白部分
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        SettingsCardRow(
            icon = Icons.Filled.SkipNext,
            title = "跳过空白部分",
            subtitle = "自动跳过音频中的静音片段",
            trailing = {
                Switch(
                    checked = skipSilence,
                    onCheckedChange = { settingsManager.setSkipSilence(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        )
    }

    // 音量提升
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showVolumeBoostDialog = true },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        SettingsCardRow(
            icon = Icons.Filled.VolumeUp,
            title = "音量提升",
            subtitle = if (volumeBoost > 0f) "+${volumeBoost.toInt()} dB" else "关闭"
        )
    }

    // 播放进度条样式
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showStyleDialog = true },
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        SettingsCardRow(
            icon = Icons.Filled.Tune,
            title = "播放进度条样式",
            subtitle = sliderStyleManager.getStyleDisplayName(currentStyle)
        )
    }

    // 音量提升调整对话框
    if (showVolumeBoostDialog) {
        VolumeBoostDialog(
            currentValue = volumeBoost,
            onValueChange = { settingsManager.setVolumeBoost(it) },
            onDismiss = { showVolumeBoostDialog = false }
        )
    }

    if (showStyleDialog) {
        com.xmvisio.app.ui.settings.SliderStyleDialog(
            currentStyle = currentStyle,
            onStyleSelected = { sliderStyleManager.setSliderStyle(it) },
            onDismiss = { showStyleDialog = false }
        )
    }
}

@Composable
private fun VolumeBoostDialog(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音量提升") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "提升音频播放音量，适用于音量较小的音频文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (sliderValue > 0f) "+${sliderValue.toInt()} dB" else "关闭",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..PlaybackSettingsManager.MAX_VOLUME_BOOST,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${PlaybackSettingsManager.MAX_VOLUME_BOOST.toInt()} dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (sliderValue > 6f) {
                    Text(
                        text = "⚠️ 过高的音量提升可能导致音质失真",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueChange(sliderValue); onDismiss() }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
