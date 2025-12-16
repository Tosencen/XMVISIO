package com.xmvisio.app.ui.audiobook

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.ui.audiobook.formatTime
import java.text.SimpleDateFormat
import java.util.*

/**
 * 音频属性对话框
 * 显示音频文件的详细信息
 */
@Composable
fun AudioPropertiesDialog(
    audio: LocalAudioFile,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 从 URI 提取文件路径
    val filePath = remember(audio.uri) {
        try {
            when {
                audio.uri.scheme == "content" -> {
                    // 尝试从 content URI 获取路径
                    val docId = DocumentsContract.getDocumentId(audio.uri)
                    if (docId.contains(":")) {
                        val split = docId.split(":")
                        if (split.size >= 2) {
                            "/storage/${split[0]}/${split[1]}"
                        } else {
                            audio.uri.toString()
                        }
                    } else {
                        audio.uri.toString()
                    }
                }
                audio.uri.scheme == "file" -> audio.uri.path ?: audio.uri.toString()
                else -> audio.uri.toString()
            }
        } catch (e: Exception) {
            audio.uri.toString()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("音频属性")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基本信息
                PropertySection(title = "基本信息") {
                    PropertyItem(label = "标题", value = audio.title)
                    audio.artist?.let {
                        PropertyItem(label = "艺术家", value = it)
                    }
                }
                
                // 文件信息
                PropertySection(title = "文件信息") {
                    PropertyItem(label = "文件名", value = audio.displayName)
                    PropertyItem(label = "时长", value = formatTime(audio.duration / 1000))
                    
                    // 文件位置 - 可点击跳转到文件夹
                    ClickablePropertyItem(
                        label = "文件位置",
                        value = filePath,
                        onClick = {
                            try {
                                // 从 URI 获取父文件夹的 URI
                                val folderUri = when {
                                    audio.uri.scheme == "content" -> {
                                        try {
                                            // 尝试获取文档树 URI
                                            val docId = DocumentsContract.getDocumentId(audio.uri)
                                            val treeUri = DocumentsContract.buildTreeDocumentUri(
                                                audio.uri.authority,
                                                docId.substringBeforeLast("/")
                                            )
                                            treeUri
                                        } catch (e: Exception) {
                                            // 如果失败，尝试构建父目录 URI
                                            val pathSegments = audio.uri.pathSegments
                                            if (pathSegments.size > 1) {
                                                val parentPath = pathSegments.dropLast(1).joinToString("/")
                                                Uri.parse("content://${audio.uri.authority}/$parentPath")
                                            } else {
                                                null
                                            }
                                        }
                                    }
                                    audio.uri.scheme == "file" -> {
                                        // 对于 file:// URI，获取父目录
                                        audio.uri.path?.let { path ->
                                            val parentPath = path.substringBeforeLast("/")
                                            Uri.parse("file://$parentPath")
                                        }
                                    }
                                    else -> null
                                }
                                
                                // 打开文件管理器到文件夹
                                if (folderUri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } else {
                                    // 如果无法获取文件夹 URI，尝试打开文件管理器根目录
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                                            DocumentsContract.Document.MIME_TYPE_DIR
                                        )
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                // 最后的备选方案：打开文件管理器应用
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage("com.android.documentsui")
                                        ?: Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                                                DocumentsContract.Document.MIME_TYPE_DIR
                                            )
                                        }
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    e2.printStackTrace()
                                }
                            }
                        }
                    )
                }
                
                // 添加日期
                PropertySection(title = "日期") {
                    PropertyItem(
                        label = "添加时间",
                        value = formatDate(audio.dateAdded * 1000)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        modifier = modifier
    )
}

/**
 * 属性分组
 */
@Composable
private fun PropertySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/**
 * 属性项
 */
@Composable
private fun PropertyItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 可点击的属性项
 */
@Composable
private fun ClickablePropertyItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "打开",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
