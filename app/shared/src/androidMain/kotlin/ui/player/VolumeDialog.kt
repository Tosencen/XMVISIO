package com.xmvisio.app.ui.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 音量控制对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // 获取当前音量和最大音量
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val safeMaxVolume = maxVolume.coerceAtLeast(1)
    val uiVolumeSteps = 20

    // 实时更新音量状态
    LaunchedEffect(Unit) {
        while (true) {
            val realVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (realVolume != currentVolume) {
                currentVolume = realVolume
            }
            delay(250)
        }
    }

    // 计算实时百分比
    val volumePercentage by remember(currentVolume, safeMaxVolume) {
        derivedStateOf { ((currentVolume.toFloat() / safeMaxVolume.toFloat()) * 100).roundToInt() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音量控制") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 音量图标 + 百分比（横向排列）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            currentVolume == 0 -> Icons.Default.VolumeMute
                            currentVolume < safeMaxVolume / 2 -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$volumePercentage%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 音量滑块
                Slider(
                    value = (currentVolume.toFloat() / safeMaxVolume) * uiVolumeSteps,
                    onValueChange = { uiValue ->
                        val normalized = (uiValue / uiVolumeSteps).coerceIn(0f, 1f)
                        val nextVolume = (normalized * safeMaxVolume).roundToInt().coerceIn(0, safeMaxVolume)
                        if (nextVolume != currentVolume) {
                            currentVolume = nextVolume
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                nextVolume,
                                0
                            )
                        }
                    },
                    valueRange = 0f..uiVolumeSteps.toFloat(),
                    steps = uiVolumeSteps - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 音量范围提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
