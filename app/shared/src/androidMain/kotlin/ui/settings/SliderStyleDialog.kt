package com.xmvisio.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.SliderStyle
import com.xmvisio.app.data.SliderStyleManager

/**
 * 播放进度条样式选择对话框
 */
@Composable
fun SliderStyleDialog(
    currentStyle: SliderStyle,
    onStyleSelected: (SliderStyle) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStyle by remember { mutableStateOf(currentStyle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放进度条样式") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SliderStyle.entries.forEach { style ->
                    StyleOption(
                        style = style,
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onStyleSelected(selectedStyle)
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
        },
        modifier = modifier
    )
}

@Composable
private fun StyleOption(
    style: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = when (style) {
        SliderStyle.DEFAULT -> "默认"
        SliderStyle.SQUIGGLY -> "波浪"
        SliderStyle.SLIM -> "纤细"
    }
    
    val description = when (style) {
        SliderStyle.DEFAULT -> "标准样式，带圆形滑块"
        SliderStyle.SQUIGGLY -> "播放时显示波浪动画"
        SliderStyle.SLIM -> "简洁样式，无滑块"
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}
