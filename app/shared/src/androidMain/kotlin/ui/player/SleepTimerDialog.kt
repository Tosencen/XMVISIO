package com.xmvisio.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 睡眠定时器底部弹窗
 * 参考 Voice 项目实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    onSetTimer: (Duration) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    
    val timerOptions = remember {
        listOf(
            5.minutes to "5分钟",
            10.minutes to "10分钟",
            15.minutes to "15分钟",
            30.minutes to "30分钟",
            45.minutes to "45分钟",
            60.minutes to "60分钟",
            90.minutes to "90分钟"
        )
    }
    
    if (showCustomDialog) {
        CustomTimerDialog(
            onConfirm = { duration ->
                onSetTimer(duration)
                onDismiss()
            },
            onDismiss = { showCustomDialog = false }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 标题
                Text(
                    text = "睡眠定时器",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                // 定时器选项列表（使用Text代替ListItem，去掉背景）
                timerOptions.forEach { (duration, label) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetTimer(duration)
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
                
                // 自定义时间选项
                Text(
                    text = "自定义时间",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showCustomDialog = true
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * 自定义时间对话框
 */
@Composable
private fun CustomTimerDialog(
    onConfirm: (Duration) -> Unit,
    onDismiss: () -> Unit
) {
    var minutesText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时间") },
        text = {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it.filter { char -> char.isDigit() } },
                label = { Text("分钟") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = minutesText.toIntOrNull()
                    if (minutes != null && minutes > 0) {
                        onConfirm(minutes.minutes)
                    }
                },
                enabled = minutesText.toIntOrNull()?.let { it > 0 } == true
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

/**
 * 睡眠定时器状态显示
 */
@Composable
fun SleepTimerIndicator(
    remainingTime: Duration,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "定时: ${formatSleepTimer(remainingTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 格式化睡眠定时器显示
 */
private fun formatSleepTimer(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    
    return if (hours > 0) {
        "${hours}小时${minutes}分钟"
    } else {
        "${minutes}分钟"
    }
}
