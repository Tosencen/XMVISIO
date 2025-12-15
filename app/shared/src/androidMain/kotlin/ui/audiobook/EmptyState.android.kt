package com.xmvisio.app.ui.audiobook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android 平台的空状态动画实现
 * 简化版本（使用图标占位符）
 */
@Composable
actual fun EmptyStateAnimation(
    modifier: Modifier,
    size: Dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = null,
            modifier = Modifier.size(size * 0.6f),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

