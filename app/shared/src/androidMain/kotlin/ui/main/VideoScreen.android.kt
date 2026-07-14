package com.xmvisio.app.ui.main

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.ui.components.UpdateButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun VideoScreen(
    onNavigateToPlayer: (VideoInfo) -> Unit,
    onNavigateToSettings: () -> Unit,
    updateAvailable: Boolean,
    onUpdateCheck: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, videoPermission) ==
            PackageManager.PERMISSION_GRANTED
    ) }
    var videos by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasPermission = true
        }
    }

    var showQuickSettings by remember { mutableStateOf(false) }
    var sortBy by rememberSaveable { mutableIntStateOf(1) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var isGridLayout by rememberSaveable { mutableStateOf(true) }

    // 持久化布局设置
    val prefs = context.getSharedPreferences("video_prefs", android.content.Context.MODE_PRIVATE)
    LaunchedEffect(Unit) {
        isGridLayout = prefs.getBoolean("grid_layout", true)
        sortBy = prefs.getInt("sort_by", 1)
        sortAscending = prefs.getBoolean("sort_asc", false)
    }
    LaunchedEffect(isGridLayout, sortBy, sortAscending) {
        prefs.edit()
            .putBoolean("grid_layout", isGridLayout)
            .putInt("sort_by", sortBy)
            .putBoolean("sort_asc", sortAscending)
            .apply()
    }

    // 长按菜单状态
    var selectedVideo by remember { mutableStateOf<VideoInfo?>(null) }
    var showVideoMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    // 排序后的视频列表
    val sortedVideos = remember(videos, sortBy, sortAscending) {
        videos.sortedWith(
            when (sortBy) {
                0 -> compareBy<VideoInfo> { it.name.lowercase() }
                1 -> compareBy<VideoInfo> { it.dateModified }
                2 -> compareBy<VideoInfo> { it.size }
                3 -> compareBy<VideoInfo> { it.duration }
                else -> compareBy<VideoInfo> { it.dateModified }
            }.let { if (sortAscending) it else it.reversed() }
        )
    }

    // 排序变化后滚动到顶部
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    LaunchedEffect(sortBy, sortAscending) {
        if (isGridLayout) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                videos = queryVideos(context)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("视频")
                        if (updateAvailable) {
                            Spacer(Modifier.width(4.dp))
                            UpdateButton(onClick = onUpdateCheck)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showQuickSettings = true }) {
                        Icon(Icons.Default.Dashboard, contentDescription = "快速设置")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                !hasPermission -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "需要授权才能访问视频文件",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionLauncher.launch(videoPermission) }
                        ) {
                            Text("授权访问视频")
                        }
                    }
                }
                videos.isEmpty() -> {
                    com.xmvisio.app.ui.components.EmptyStateView(
                        message = "没有找到视频文件"
                    )
                }
                else -> {
                    if (isGridLayout) {
                        VideoGrid(
                            videos = sortedVideos,
                            state = gridState,
                            onVideoClick = onNavigateToPlayer,
                            onVideoLongClick = { video ->
                                selectedVideo = video
                                showVideoMenu = true
                            }
                        )
                    } else {
                        VideoList(
                            videos = sortedVideos,
                            state = listState,
                            onVideoClick = onNavigateToPlayer,
                            onVideoLongClick = { video ->
                                selectedVideo = video
                                showVideoMenu = true
                            }
                        )
                    }
                }
            }
        }

        if (showQuickSettings) {
            AlertDialog(
                onDismissRequest = { showQuickSettings = false },
                title = { Text("快速设置") },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    // 布局模式
                    Text("布局", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = isGridLayout,
                            onClick = { isGridLayout = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("网格") }
                        SegmentedButton(
                            selected = !isGridLayout,
                            onClick = { isGridLayout = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("列表") }
                    }

                    // 排序方式
                    Text("排序方式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = sortBy == 0,
                            onClick = { sortBy = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) { Text("名称") }
                        SegmentedButton(
                            selected = sortBy == 1,
                            onClick = { sortBy = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) { Text("日期") }
                        SegmentedButton(
                            selected = sortBy == 2,
                            onClick = { sortBy = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) { Text("大小") }
                        SegmentedButton(
                            selected = sortBy == 3,
                            onClick = { sortBy = 3 },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) { Text("时长") }
                    }

                    // 排序方向
                    Text("排序方向", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !sortAscending,
                            onClick = { sortAscending = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("降序") }
                        SegmentedButton(
                            selected = sortAscending,
                            onClick = { sortAscending = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("升序") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSettings = false }) { Text("完成") }
            }
        )
    }

    // === 视频长按菜单 ===
    if (showVideoMenu && selectedVideo != null) {
        val sheetState = rememberModalBottomSheetState()
        val video = selectedVideo!!

        ModalBottomSheet(
            onDismissRequest = { showVideoMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(onClick = {
                    renameText = video.name
                    showRenameDialog = true
                    showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("重命名", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Surface(onClick = {
                    showPropertiesDialog = true
                    showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("属性", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Surface(onClick = {
                    showDeleteDialog = true
                    showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("删除", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // 重命名对话框
    if (showRenameDialog && selectedVideo != null) {
        val video = selectedVideo!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("新名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val extension = video.name.substringAfterLast(".", "")
                        val finalName = if (extension.isNotEmpty()) "$renameText.$extension" else renameText
                        try {
                            withContext(Dispatchers.IO) {
                                val values = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, finalName)
                                }
                                context.contentResolver.update(android.net.Uri.parse(video.uri), values, null, null)
                            }
                        } catch (_: Exception) { }
                        showRenameDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && selectedVideo != null) {
        val video = selectedVideo!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除视频") },
            text = {
                Text("确定要删除「${video.name}」吗？此操作无法撤销。")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                context.contentResolver.delete(android.net.Uri.parse(video.uri), null, null)
                            }
                            sortedVideos.let { }
                        } catch (_: Exception) { }
                        showDeleteDialog = false
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    // 属性对话框
    if (showPropertiesDialog && selectedVideo != null) {
        val video = selectedVideo!!
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            title = { Text("视频属性") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column { Text("名称", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(video.name, style = MaterialTheme.typography.bodyMedium) }
                    Column { Text("时长", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(video.formattedDuration, style = MaterialTheme.typography.bodyMedium) }
                    Column { Text("大小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(formatFileSize(video.size), style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) { Text("关闭") }
            }
        )
    }
}

}

@Composable
private fun VideoGrid(
    videos: List<VideoInfo>,
    state: LazyGridState,
    onVideoClick: (VideoInfo) -> Unit,
    onVideoLongClick: (VideoInfo) -> Unit
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            VideoCard(video = video, onClick = { onVideoClick(video) }, onLongClick = { onVideoLongClick(video) })
        }
    }
}

@Composable
private fun VideoList(
    videos: List<VideoInfo>,
    state: LazyListState,
    onVideoClick: (VideoInfo) -> Unit,
    onVideoLongClick: (VideoInfo) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        state = state,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            VideoListItem(video = video, onClick = { onVideoClick(video) }, onLongClick = { onVideoLongClick(video) })
        }
    }
}

@Composable
private fun VideoListItem(
    video: VideoInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        withContext(Dispatchers.IO) {
            thumbnailBitmap = loadVideoThumbnail(context, video.id)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailBitmap != null) {
                Image(
                    painter = BitmapPainter(thumbnailBitmap!!.asImageBitmap()),
                    contentDescription = video.name,
                    modifier = Modifier
                        .size(120.dp, 68.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp, 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        withContext(Dispatchers.IO) {
            thumbnailBitmap = loadVideoThumbnail(context, video.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box {
            if (thumbnailBitmap != null) {
                Image(
                    painter = BitmapPainter(thumbnailBitmap!!.asImageBitmap()),
                    contentDescription = video.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = video.formattedDuration,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Text(
            text = video.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private suspend fun queryVideos(context: Context): List<VideoInfo> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<VideoInfo>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_MODIFIED,
    )
    val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val name = cursor.getString(nameIndex) ?: continue
            val duration = cursor.getLong(durationIndex)
            val size = cursor.getLong(sizeIndex)
            val dateModified = cursor.getLong(dateIndex)
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
            ).toString()

            videos.add(
                VideoInfo(
                    id = id,
                    uri = uri,
                    name = name,
                    duration = duration,
                    size = size,
                    dateModified = dateModified
                )
            )
        }
    }

    return@withContext videos
}

private suspend fun loadVideoThumbnail(
    context: Context,
    videoId: Long
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId
            )
            context.contentResolver.loadThumbnail(uri, android.util.Size(512, 384), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}
