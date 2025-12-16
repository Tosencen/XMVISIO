package com.xmvisio.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.SliderStyleManager
import com.xmvisio.app.update.UpdateViewModel

/**
 * Android 平台的更新对话框实现
 */
@Composable
actual fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
) {
    UpdateDialog(
        onDismiss = onDismiss,
        updateViewModel = updateViewModel as UpdateViewModel
    )
}

/**
 * Android 平台创建 UpdateViewModel
 */
@Composable
actual fun rememberUpdateViewModel(): Any {
    val context = LocalContext.current
    return remember { UpdateViewModel(context) }
}

/**
 * 播放进度条样式设置区域（Android 实现）
 */
@Composable
actual fun SliderStyleSection() {
    val context = LocalContext.current
    val sliderStyleManager = remember { SliderStyleManager.getInstance(context) }
    val currentStyle by sliderStyleManager.sliderStyle.collectAsState()
    var showStyleDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = { showStyleDialog = true },
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "播放进度条样式",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = sliderStyleManager.getStyleDisplayName(currentStyle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // 样式选择对话框
    if (showStyleDialog) {
        SliderStyleDialog(
            currentStyle = currentStyle,
            onStyleSelected = { style ->
                sliderStyleManager.setSliderStyle(style)
            },
            onDismiss = { showStyleDialog = false }
        )
    }
}
