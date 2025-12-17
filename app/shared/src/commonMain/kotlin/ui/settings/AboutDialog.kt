package com.xmvisio.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 关于 XMVISIO 对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "XMVISIO",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 版本信息
                InfoSection(
                    icon = Icons.Default.Tag,
                    title = "版本信息",
                    content = "v$currentVersion"
                )
                
                HorizontalDivider()
                
                // 应用描述
                InfoSection(
                    icon = Icons.Default.Description,
                    title = "应用简介",
                    content = "XMVISIO 是一款基于 Kotlin Multiplatform 和 Compose Multiplatform 构建的现代化媒体应用。支持本地音频播放、有声书管理等功能。"
                )
                
                HorizontalDivider()
                
                // 主要特性
                InfoSection(
                    icon = Icons.Default.Star,
                    title = "主要特性",
                    content = """
                        • 📱 原生 Android 应用
                        • 🎨 Material 3 现代 UI
                        • 🌓 深色/浅色主题切换
                        • 🎵 本地音频播放
                        • 📚 有声书管理
                        • 🔄 自动更新检测
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                // 技术栈
                InfoSection(
                    icon = Icons.Default.Code,
                    title = "技术栈",
                    content = """
                        • Kotlin Multiplatform
                        • Compose Multiplatform
                        • Material 3 Design
                        • Coroutines & Flow
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                // 版权信息
                InfoSection(
                    icon = Icons.Default.Copyright,
                    title = "版权信息",
                    content = "© 2025 XMVISIO\nMIT License"
                )
                
                HorizontalDivider()
                
                // GitHub 仓库
                InfoSection(
                    icon = Icons.Default.Link,
                    title = "开源仓库",
                    content = "github.com/Tosencen/XMVISIO"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 信息区块组件
 */
@Composable
private fun InfoSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
