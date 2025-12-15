package com.xmvisio.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat

/**
 * 播放速度调整底部弹窗
 * 参考 Voice 项目实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speed by remember { mutableStateOf(currentSpeed) }
    val speedFormatter = remember { DecimalFormat("0.00 x") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = "播放速度",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 当前速度显示
            Text(
                text = speedFormatter.format(speed),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            // 速度滑块 (0.5x - 3.0x)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = speed,
                    onValueChange = { 
                        speed = it
                        onSpeedChange(it)
                    },
                    valueRange = 0.5f..3.0f,
                    steps = 49, // 0.05的步长，共50个值
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0.5x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "3.0x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 快捷速度按钮 - 水平排列，文字水平显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { presetSpeed ->
                    FilledTonalButton(
                        onClick = {
                            speed = presetSpeed
                            onSpeedChange(presetSpeed)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (presetSpeed == 1.0f) "正常" else "${presetSpeed}x",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
            
            // 确定按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("确定")
            }
        }
    }
}
