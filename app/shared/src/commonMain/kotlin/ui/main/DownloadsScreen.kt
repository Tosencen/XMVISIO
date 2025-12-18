package com.xmvisio.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 下载页面
 * 右上角包含设置按钮
 */
@Composable
expect fun DownloadsScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
)
