package com.xmvisio.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 睡眠定时器对话框
 * 参考 XMSLEEP 项目实现：AlertDialog + Slider + 水平排列的预设时间按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    onSetTimer: (Duration) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by remember { mutableStateOf(30) }
    
    // 预设时间选项（分钟）- 15分钟、30分钟、45分钟、1小时、1.5小时、2小时
    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时器") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 预设时间快速选择
                Text(
                    text = "快速选择",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 将6个选项分成两行，每行3个
                val rows = presetMinutes.chunked(3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { minutes ->
                                FilterChip(
                                    selected = selectedMinutes == minutes,
                                    onClick = { selectedMinutes = minutes },
                                    label = {
                                        Text(
                                            text = when {
                                                minutes >= 60 -> {
                                                    val hours = minutes / 60
                                                    val mins = minutes % 60
                                                    if (mins > 0) {
                                                        "${hours}.${mins / 6}小时"
                                                    } else {
                                                        "${hours}小时"
                                                    }
                                                }
                                                else -> "${minutes}分钟"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 滑杆调节
                Text(
                    text = "精确调节",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt() },
                    valueRange = 5f..180f,
                    steps = 34, // 每5分钟一个步长 (5, 10, 15, ..., 180)
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "5分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedMinutes >= 60) {
                            val hours = selectedMinutes / 60
                            val mins = selectedMinutes % 60
                            if (mins > 0) {
                                "${hours}小时${mins}分钟"
                            } else {
                                "${hours}小时"
                            }
                        } else {
                            "${selectedMinutes}分钟"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "3小时",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSetTimer(selectedMinutes.minutes)
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


