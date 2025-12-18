package com.xmvisio.app.ui.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.download.DownloadStatus
import com.xmvisio.app.download.DownloadTask
import com.xmvisio.app.download.IDownloadManager
import com.xmvisio.app.download.YtDlpUpdateStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealDownloadScreen(
    downloadManager: IDownloadManager,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by downloadManager.downloads.collectAsState()
    var hasPermission by remember { mutableStateOf(downloadManager.hasStoragePermission()) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var ytdlpVersion by remember { mutableStateOf(downloadManager.getYtDlpVersion()) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("下载") },
                actions = {
                    // 菜单按钮
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Info, contentDescription = "信息")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // yt-dlp 版本信息
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "yt-dlp 版本",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            ytdlpVersion ?: "未知",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = { },
                                enabled = false
                            )
                            Divider()
                            // 更新 yt-dlp
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isUpdating) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(if (isUpdating) "更新中..." else "检查更新")
                                    }
                                },
                                onClick = {
                                    if (!isUpdating) {
                                        isUpdating = true
                                        updateMessage = null
                                        scope.launch {
                                            try {
                                                val status = downloadManager.updateYtDlp()
                                                updateMessage = when (status) {
                                                    YtDlpUpdateStatus.DONE -> {
                                                        ytdlpVersion = downloadManager.getYtDlpVersion()
                                                        "更新成功"
                                                    }
                                                    YtDlpUpdateStatus.ALREADY_UP_TO_DATE -> "已是最新版本"
                                                    else -> "更新失败"
                                                }
                                            } catch (e: Exception) {
                                                updateMessage = "更新失败: ${e.message}"
                                            } finally {
                                                isUpdating = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isUpdating
                            )
                            if (updateMessage != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            updateMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                            }
                        }
                    }
                    
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Download, contentDescription = "添加下载")
            }
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        "暂无下载任务",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "点击 + 按钮添加下载",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "支持 YouTube, SoundCloud 等平台",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "文件将保存到应用存储目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { task ->
                    DownloadTaskCard(
                        task = task,
                        onCancel = { downloadManager.cancelDownload(task.id) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        var selectedType by remember { mutableStateOf(com.xmvisio.app.download.DownloadType.AUDIO) }
        val context = LocalContext.current
        
        AlertDialog(
            onDismissRequest = { if (!isDownloading) showAddDialog = false },
            title = { Text("添加下载任务") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "请输入视频链接",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { newValue ->
                            // 自动识别并提取 URL
                            val urls = com.xmvisio.app.util.UrlUtil.findURLsFromString(newValue)
                            urlInput = if (urls.isNotEmpty()) {
                                urls.first() // 只取第一个 URL
                            } else {
                                newValue // 如果没有识别到 URL，保留原始输入
                            }
                        },
                        label = { Text("视频链接") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading,
                        singleLine = false,
                        maxLines = 3,
                        supportingText = {
                            if (urlInput.isNotEmpty() && !com.xmvisio.app.util.UrlUtil.containsUrl(urlInput)) {
                                Text(
                                    "未识别到有效的 URL",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        isError = urlInput.isNotEmpty() && !com.xmvisio.app.util.UrlUtil.containsUrl(urlInput)
                    )
                    
                    // 下载类型选择
                    Text(
                        "下载类型",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == com.xmvisio.app.download.DownloadType.AUDIO,
                            onClick = { selectedType = com.xmvisio.app.download.DownloadType.AUDIO },
                            label = { Text("仅音频") },
                            leadingIcon = if (selectedType == com.xmvisio.app.download.DownloadType.AUDIO) {
                                { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == com.xmvisio.app.download.DownloadType.VIDEO,
                            onClick = { selectedType = com.xmvisio.app.download.DownloadType.VIDEO },
                            label = { Text("视频") },
                            leadingIcon = if (selectedType == com.xmvisio.app.download.DownloadType.VIDEO) {
                                { Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (isDownloading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text(
                                "正在处理...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧粘贴按钮
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    // 只提取 URL 部分，忽略其他字符
                                    val extractedUrl = com.xmvisio.app.util.UrlUtil.extractFirstUrl(text)
                                    if (extractedUrl != null) {
                                        urlInput = extractedUrl
                                    } else {
                                        // 如果没有找到 URL，保留原始文本
                                        urlInput = text
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "粘贴",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("粘贴")
                    }
                    
                    // 右侧按钮组
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            enabled = !isDownloading
                        ) {
                            Text("取消")
                        }
                        TextButton(
                            onClick = {
                                if (urlInput.isNotBlank() && !isDownloading) {
                                    isDownloading = true
                                    showAddDialog = false  // 立即关闭弹窗
                                    scope.launch {
                                        try {
                                            downloadManager.startDownload(urlInput, selectedType)
                                            urlInput = ""
                                        } catch (e: Exception) {
                                            // 错误已在 manager 中处理
                                        } finally {
                                            isDownloading = false
                                        }
                                    }
                                }
                            },
                            enabled = urlInput.isNotBlank() && !isDownloading
                        ) {
                            Text("开始下载")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                    if (task.author != null) {
                        Text(
                            text = task.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                IconButton(
                    onClick = onCancel,
                    enabled = task.status == DownloadStatus.DOWNLOADING || 
                             task.status == DownloadStatus.PENDING ||
                             task.status == DownloadStatus.EXTRACTING
                ) {
                    Icon(
                        when (task.status) {
                            DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                            DownloadStatus.FAILED -> Icons.Default.Error
                            DownloadStatus.CANCELLED -> Icons.Default.Cancel
                            else -> Icons.Default.Close
                        },
                        contentDescription = "操作",
                        tint = when (task.status) {
                            DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            // 状态文本
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val statusText = when (task.status) {
                    DownloadStatus.PENDING -> "等待中..."
                    DownloadStatus.EXTRACTING -> "正在解析视频信息..."
                    DownloadStatus.DOWNLOADING -> {
                        val percentage = (task.progress * 100).toInt().coerceAtLeast(0)
                        if (task.totalBytes > 0) {
                            val downloadedMB = task.downloadedBytes / (1024f * 1024f)
                            val totalMB = task.totalBytes / (1024f * 1024f)
                            "下载中 $percentage% (${"%.1f".format(downloadedMB)} MB / ${"%.1f".format(totalMB)} MB)"
                        } else if (percentage > 0) {
                            "下载中 $percentage%"
                        } else {
                            "正在下载..."
                        }
                    }
                    DownloadStatus.COMPLETED -> "✓ 下载完成"
                    DownloadStatus.FAILED -> "✗ ${task.errorMessage ?: "下载失败"}"
                    DownloadStatus.CANCELLED -> "已取消"
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (task.status) {
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
                
                // 显示文件保存位置
                if (task.status == DownloadStatus.COMPLETED && task.filePath != null) {
                    Text(
                        text = "保存位置: ${task.filePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 2
                    )
                }
            }
            
            // 进度条
            if (task.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (task.status == DownloadStatus.EXTRACTING || task.status == DownloadStatus.PENDING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
