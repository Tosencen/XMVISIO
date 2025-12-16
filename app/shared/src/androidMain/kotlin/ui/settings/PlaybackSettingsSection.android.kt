package com.xmvisio.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.PlaybackSettingsManager

/**
 * 播放设置区域（Android 实现）
 */
@Composable
actual fun PlaybackSettingsSection() {
    val context = LocalContext.current
    val settingsManager = remember { PlaybackSettingsManager.getInstance(context) }
    
    val skipSilence by settingsManager.skipSilence.collectAsState()
    val volumeBoost by settingsManager.volumeBoost.collectAsState()
    
    var showVolumeBoostDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 跳过空白部分
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "跳过空白部分",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "自动跳过音频中的静音片段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = skipSilence,
                    onCheckedChange = { settingsManager.setSkipSilence(it) }
                )
            }
        }
        
        // 音量提升
        Card(
            onClick = { showVolumeBoostDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "音量提升",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (volumeBoost > 0f) {
                                "+${volumeBoost.toInt()} dB"
                            } else {
                                "关闭"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // 音量提升调整对话框
    if (showVolumeBoostDialog) {
        VolumeBoostDialog(
            currentValue = volumeBoost,
            onValueChange = { settingsManager.setVolumeBoost(it) },
            onDismiss = { showVolumeBoostDialog = false }
        )
    }
}

/**
 * 音量提升调整对话框
 */
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
                
                // 当前值显示
                Text(
                    text = if (sliderValue > 0f) {
                        "+${sliderValue.toInt()} dB"
                    } else {
                        "关闭"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                // 滑块
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..PlaybackSettingsManager.MAX_VOLUME_BOOST,
                        steps = 8, // 0, 1, 2, ..., 9
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0 dB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${PlaybackSettingsManager.MAX_VOLUME_BOOST.toInt()} dB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            TextButton(
                onClick = {
                    onValueChange(sliderValue)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
