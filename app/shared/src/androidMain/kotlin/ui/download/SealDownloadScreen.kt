package com.xmvisio.app.ui.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.download.DownloadStatus
import com.xmvisio.app.download.DownloadTask
import com.xmvisio.app.download.DownloadType
import com.xmvisio.app.download.IDownloadManager
import com.xmvisio.app.download.YtDlpAutoUpdater
import com.xmvisio.app.download.YtDlpUpdateStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealDownloadScreen(
    downloadManager: IDownloadManager,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloads by downloadManager.downloads.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var ytdlpVersion by remember { mutableStateOf(downloadManager.getYtDlpVersion()) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // 自动检查 yt-dlp 更新（每周一次）
    val autoUpdater = remember { YtDlpAutoUpdater(context) }
    LaunchedEffect(Unit) {
        autoUpdater.checkAndUpdateIfNeeded(downloadManager)
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = { Text("下载") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
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
                            HorizontalDivider()
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
                            
                            // 清理下载记录
                            if (downloads.isNotEmpty()) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("清理已完成")
                                        }
                                    },
                                    onClick = {
                                        downloadManager.clearCompletedDownloads()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("清理全部记录")
                                        }
                                    },
                                    onClick = {
                                        downloadManager.clearAllDownloads()
                                        showMenu = false
                                    }
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "添加下载"
                )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "暂无下载任务",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "点击右下角按钮添加下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                        onCancel = { downloadManager.cancelDownload(task.id) },
                        onDismiss = { downloadManager.removeDownload(task.id) },
                        onRetry = { scope.launch { downloadManager.retryDownload(task.id) } }
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
                        trailingIcon = {
                            if (urlInput.isNotEmpty() && !isDownloading) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
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
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                        FilterChip(
                            selected = selectedType == com.xmvisio.app.download.DownloadType.VIDEO,
                            onClick = { selectedType = com.xmvisio.app.download.DownloadType.VIDEO },
                            label = { Text("视频") },
                            leadingIcon = if (selectedType == com.xmvisio.app.download.DownloadType.VIDEO) {
                                { Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current
    
    // 只有已完成、失败、取消的任务才能左滑删除
    val canDismiss = task.status == DownloadStatus.COMPLETED ||
                     task.status == DownloadStatus.FAILED ||
                     task.status == DownloadStatus.CANCELLED
    
    if (canDismiss) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
        )
        
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            backgroundContent = {
                // 左滑时显示的背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true
        ) {
            DownloadTaskCardContent(
                task = task,
                onCancel = onCancel,
                onRetry = onRetry,
                contentColor = contentColor,
                context = context
            )
        }
    } else {
        DownloadTaskCardContent(
            task = task,
            onCancel = onCancel,
            onRetry = onRetry,
            contentColor = contentColor,
            context = context,
            modifier = modifier
        )
    }
}

@Composable
private fun DownloadTaskCardContent(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp
        ),
        onClick = {
            // 仅当下载完成且是音频类型时可点击播放
            if (task.status == DownloadStatus.COMPLETED && 
                task.downloadType == DownloadType.AUDIO && 
                task.filePath != null) {
                try {
                    // 使用系统默认播放器打开音频文件
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        val uri = if (task.filePath.startsWith("content://")) {
                            android.net.Uri.parse(task.filePath)
                        } else {
                            android.net.Uri.parse("file://${task.filePath}")
                        }
                        setDataAndType(uri, "audio/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("DownloadTaskCard", "Failed to play audio", e)
                }
            }
        }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：封面
                    Box(
                        modifier = Modifier.size(56.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (task.downloadType == DownloadType.VIDEO) {
                                        Icons.Default.VideoLibrary
                                    } else {
                                        Icons.Default.AudioFile
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
            
                // 中间：信息
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),  // 与封面高度一致，确保卡片高度统一
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 标题
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                
                    // 作者
                    if (task.author != null) {
                        Text(
                            text = task.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                
                    // 状态行
                    Row(
                        modifier = Modifier.height(32.dp),  // 固定高度统一所有状态
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (task.status) {
                            DownloadStatus.COMPLETED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "完成",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "下载完成",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // 时长和大小
                                if (task.duration > 0 || task.fileSize > 0) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.5f)
                                    )
                                    if (task.duration > 0) {
                                        val minutes = task.duration / 60
                                        val seconds = task.duration % 60
                                        Text(
                                            text = "${minutes}:${seconds.toString().padStart(2, '0')}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (task.duration > 0 && task.fileSize > 0) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.5f)
                                        )
                                    }
                                    if (task.fileSize > 0) {
                                        val sizeMB = task.fileSize / (1024f * 1024f)
                                        Text(
                                            text = "${"%.1f".format(sizeMB)} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            DownloadStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "错误",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = task.errorMessage ?: "下载失败",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DownloadStatus.CANCELLED -> {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "已取消",
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "已取消",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                            DownloadStatus.PENDING -> {
                                Text(
                                    text = "等待中...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                            DownloadStatus.EXTRACTING -> {
                                Text(
                                    text = "正在解析视频信息...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                            DownloadStatus.DOWNLOADING -> {
                                val percentage = (task.progress * 100).toInt().coerceAtLeast(0)
                                val statusText = if (task.totalBytes > 0) {
                                    val downloadedMB = task.downloadedBytes / (1024f * 1024f)
                                    val totalMB = task.totalBytes / (1024f * 1024f)
                                    "$percentage% • ${"%.1f".format(downloadedMB)}/${"%.1f".format(totalMB)} MB"
                                } else if (percentage > 0) {
                                    "$percentage%"
                                } else {
                                    "正在下载..."
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    
                        // 取消按钮
                        if (task.status == DownloadStatus.DOWNLOADING || 
                            task.status == DownloadStatus.PENDING ||
                            task.status == DownloadStatus.EXTRACTING) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = onCancel,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = contentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("取消", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                }
                
                // 右上角重试按钮（仅在下载失败时显示）
                if (task.status == DownloadStatus.FAILED) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重试下载",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // 进度条（在卡片底部）- 所有状态都保留4dp高度以统一卡片高度
            when {
                task.status == DownloadStatus.DOWNLOADING -> {
                    val progress = task.progress.coerceIn(0f, 1f)
                    if (progress > 0) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
                task.status == DownloadStatus.EXTRACTING || task.status == DownloadStatus.PENDING -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                }
                else -> {
                    // 完成/失败/取消状态：使用透明占位保持高度一致
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
