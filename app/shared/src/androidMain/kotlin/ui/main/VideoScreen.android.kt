package com.xmvisio.app.ui.main

import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.MediaHolder
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.ui.components.UpdateButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun VideoScreen(
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit,
    onNavigateToSettings: () -> Unit,
    updateAvailable: Boolean,
    onUpdateCheck: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    // ===== 状态持有者（ViewModel，跨组合存活；先创建，稍后绑定 intent launcher）=====
    val state: VideoScreenState = viewModel {
        VideoScreenState(context.applicationContext)
    }

    // ===== 权限请求 =====
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) state.hasPermission = true
    }

    // ===== MediaStore 写/删操作请求（Android 10+）=====
    val mediaRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        state.handleMediaRequestResult(result.resultCode)
    }

    // 绑定 intent launcher 到 state
    LaunchedEffect(Unit) {
        state.onLaunchIntent = { mediaRequestLauncher.launch(it) }
    }

    // ===== 加载持久化设置 =====
    LaunchedEffect(Unit) { state.loadPersistedSettings() }

    // ===== 持久化设置变化 =====
    LaunchedEffect(state.isGridLayout, state.sortBy, state.sortAscending, state.viewMode) {
        state.persistSettings()
    }

    // ===== 加载视频 =====
    LaunchedEffect(state.hasPermission, state.refreshTrigger) {
        state.loadVideos()
    }

    // ===== ContentObserver：监听 MediaStore 视频变化 =====
    DisposableEffect(state.hasPermission) {
        if (!state.hasPermission) return@DisposableEffect onDispose {}
        val observer = state.createContentObserver()
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        context.contentResolver.registerContentObserver(videoUri, true, observer)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    // ===== 返回键：在文件夹中时逐级返回 =====
    BackHandler(enabled = state.currentFolderPath != null) {
        state.onBackPressed()
    }

    // ===== 排序变化后滚动到顶部 =====
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.sortBy, state.sortAscending) {
        if (state.isGridLayout) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
    }

    // ==================== UI 渲染 ====================
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (state.viewMode) {
                                0 -> "视频"
                                1 -> "文件夹"
                                else -> "全部视频"
                            }
                        )
                        if (updateAvailable) {
                            Spacer(Modifier.width(4.dp))
                            UpdateButton(onClick = onUpdateCheck)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { state.showQuickSettings = true }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ===== 面包屑导航栏 =====
            if (state.viewMode != 2 && state.breadcrumbPath.isNotEmpty()) {
                BreadcrumbBar(
                    breadcrumbPath = state.breadcrumbPath,
                    onNavigateBack = { state.navigateToParentFolder() },
                    onNavigateToPath = { path ->
                        state.currentFolderPath = if (path == null) null else path
                    }
                )
            }

            // ===== 主内容区域 =====
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { state.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    !state.hasPermission -> {
                        PermissionPlaceholder(
                            onRequestPermission = { permissionLauncher.launch(state.videoPermission) }
                        )
                    }
                    state.videos.isEmpty() -> {
                        EmptyVideoPlaceholder()
                    }
                    else -> {
                        VideoContent(
                            state = state,
                            listState = listState,
                            gridState = gridState,
                            onNavigateToPlayer = onNavigateToPlayer,
                            sortedVideos = state.sortedVideos
                        )
                    }
                }
            }
        }
    }

    // ===== 对话框和底部弹窗 =====
    QuickSettingsDialog(state = state)
    VideoMenuSheet(state = state, onRename = { state.showRenameDialog = true }, onDelete = { state.showDeleteDialog = true })
    FolderMenuSheet(state = state)
    FolderRenameDialog(state = state)
    FolderDeleteDialog(state = state)
    VideoRenameDialog(state = state)
    VideoDeleteDialog(state = state)
    VideoPropertiesDialog(state = state)
}

// ====================================================================
//  UI 子组件
// ====================================================================

/**
 * 面包屑导航栏
 */
@Composable
private fun BreadcrumbBar(
    breadcrumbPath: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToPath: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "返回上级",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        breadcrumbPath.forEachIndexed { index, path ->
            if (index > 0) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val isLast = index == breadcrumbPath.lastIndex
            Text(
                text = java.io.File(path).name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNavigateToPath(if (index == 0) null else path) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * 权限提示占位
 */
@Composable
private fun PermissionPlaceholder(onRequestPermission: () -> Unit) {
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
        Button(onClick = onRequestPermission) {
            Text("授权访问视频")
        }
    }
}

/**
 * 空视频占位
 */
@Composable
private fun EmptyVideoPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = folderPainter(),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "没有找到视频文件",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 视频主内容（根据 viewMode 渲染不同布局）
 */
@Composable
private fun VideoContent(
    state: VideoScreenState,
    listState: LazyListState,
    gridState: LazyGridState,
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit,
    sortedVideos: List<VideoInfo>
) {
    when (state.viewMode) {
        0 -> TreeView(
            state = state,
            onNavigateToPlayer = onNavigateToPlayer,
            sortedVideos = sortedVideos,
            isGridLayout = state.isGridLayout
        )
        1 -> FolderView(
            state = state,
            onNavigateToPlayer = onNavigateToPlayer,
            sortedVideos = sortedVideos,
            isGridLayout = state.isGridLayout
        )
        else -> AllVideosView(
            state = state,
            listState = listState,
            gridState = gridState,
            onNavigateToPlayer = onNavigateToPlayer,
            sortedVideos = sortedVideos,
            isGridLayout = state.isGridLayout
        )
    }
}

@Composable
private fun TreeView(
    state: VideoScreenState,
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit,
    sortedVideos: List<VideoInfo>,
    isGridLayout: Boolean
) {
    val mediaHolder = MediaHolder(
        folders = state.displayFolders,
        videos = state.currentFolderVideos
    )
    com.xmvisio.app.ui.folder.MediaView(
        mediaHolder = mediaHolder,
        isGridLayout = isGridLayout,
        isFolderTreeMode = true,
        onFolderClick = { folder -> state.currentFolderPath = folder.path },
        onFolderLongClick = { folder ->
            state.selectedFolder = folder
            state.showFolderMenu = true
        },
        onVideoClick = { video -> onNavigateToPlayer(video, sortedVideos) },
        onVideoLongClick = { video ->
            state.selectedVideo = video
            state.showVideoMenu = true
        },
        onAudioClick = {},
        onAudioLongClick = null
    )
}

@Composable
private fun FolderView(
    state: VideoScreenState,
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit,
    sortedVideos: List<VideoInfo>,
    isGridLayout: Boolean
) {
    if (state.displayFolders.isNotEmpty()) {
        val mediaHolder = MediaHolder(
            folders = state.displayFolders,
            videos = if (state.currentFolderPath != null) state.currentFolderVideos else emptyList()
        )
        com.xmvisio.app.ui.folder.MediaView(
            mediaHolder = mediaHolder,
            isGridLayout = isGridLayout,
            isFolderTreeMode = false,
            onFolderClick = { folder -> state.currentFolderPath = folder.path },
            onFolderLongClick = { folder ->
                state.selectedFolder = folder
                state.showFolderMenu = true
            },
            onVideoClick = { video -> onNavigateToPlayer(video, sortedVideos) },
            onVideoLongClick = { video ->
                state.selectedVideo = video
                state.showVideoMenu = true
            },
            onAudioClick = {},
            onAudioLongClick = null
        )
    } else {
        val mediaHolder = MediaHolder(videos = state.currentFolderVideos)
        com.xmvisio.app.ui.folder.MediaView(
            mediaHolder = mediaHolder,
            isGridLayout = isGridLayout,
            isFolderTreeMode = false,
            onFolderClick = {},
            onFolderLongClick = null,
            onVideoClick = { video -> onNavigateToPlayer(video, sortedVideos) },
            onVideoLongClick = { video ->
                state.selectedVideo = video
                state.showVideoMenu = true
            },
            onAudioClick = {},
            onAudioLongClick = null
        )
    }
}

@Composable
private fun AllVideosView(
    state: VideoScreenState,
    listState: LazyListState,
    gridState: LazyGridState,
    onNavigateToPlayer: (VideoInfo, List<VideoInfo>) -> Unit,
    sortedVideos: List<VideoInfo>,
    isGridLayout: Boolean
) {
    if (isGridLayout) {
        VideoGrid(
            videos = sortedVideos,
            state = gridState,
            onVideoClick = { video -> onNavigateToPlayer(video, sortedVideos) },
            onVideoLongClick = { video ->
                state.selectedVideo = video
                state.showVideoMenu = true
            }
        )
    } else {
        VideoList(
            videos = sortedVideos,
            state = listState,
            onVideoClick = { video -> onNavigateToPlayer(video, sortedVideos) },
            onVideoLongClick = { video ->
                state.selectedVideo = video
                state.showVideoMenu = true
            }
        )
    }
}

// ====================================================================
//  对话框 & 底部弹窗
// ====================================================================

@Composable
private fun QuickSettingsDialog(state: VideoScreenState) {
    if (state.showQuickSettings) {
        AlertDialog(
            onDismissRequest = { state.showQuickSettings = false },
            title = { Text("快速设置") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 浏览模式
                    Text("浏览模式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.viewMode == 0,
                            onClick = { state.viewMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("树形目录") }
                        SegmentedButton(
                            selected = state.viewMode == 1,
                            onClick = { state.viewMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("文件夹") }
                        SegmentedButton(
                            selected = state.viewMode == 2,
                            onClick = { state.viewMode = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("全部") }
                    }
                    // 布局
                    Text("布局", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.isGridLayout,
                            onClick = { state.isGridLayout = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("网格") }
                        SegmentedButton(
                            selected = !state.isGridLayout,
                            onClick = { state.isGridLayout = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("列表") }
                    }
                    // 排序方式
                    Text("排序方式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.sortBy == 0,
                            onClick = { state.sortBy = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) { Text("名称") }
                        SegmentedButton(
                            selected = state.sortBy == 1,
                            onClick = { state.sortBy = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) { Text("日期") }
                        SegmentedButton(
                            selected = state.sortBy == 2,
                            onClick = { state.sortBy = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) { Text("大小") }
                        SegmentedButton(
                            selected = state.sortBy == 3,
                            onClick = { state.sortBy = 3 },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) { Text("时长") }
                    }
                    // 排序方向
                    Text("排序方向", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !state.sortAscending,
                            onClick = { state.sortAscending = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("降序") }
                        SegmentedButton(
                            selected = state.sortAscending,
                            onClick = { state.sortAscending = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("升序") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { state.showQuickSettings = false }) { Text("完成") }
            }
        )
    }
}

@Composable
private fun VideoMenuSheet(
    state: VideoScreenState,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    if (state.showVideoMenu && state.selectedVideo != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { state.showVideoMenu = false },
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
                    text = state.selectedVideo!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(onClick = {
                    state.renameText = ""
                    onRename()
                    state.showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("重命名", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(onClick = {
                    state.showPropertiesDialog = true
                    state.showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("属性", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(onClick = {
                    onDelete()
                    state.showVideoMenu = false
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
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
}

@Composable
private fun FolderMenuSheet(state: VideoScreenState) {
    if (state.showFolderMenu && state.selectedFolder != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { state.showFolderMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = state.selectedFolder!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(onClick = {
                    state.folderRenameText = state.selectedFolder!!.name
                    state.showFolderMenu = false
                    state.showFolderRenameDialog = true
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("重命名", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(onClick = { state.showFolderMenu = false }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("属性", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${state.selectedFolder!!.mediaCount} 个文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(onClick = {
                    state.showFolderMenu = false
                    state.showFolderDeleteDialog = true
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
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
}

@Composable
private fun FolderRenameDialog(state: VideoScreenState) {
    if (state.showFolderRenameDialog && state.selectedFolder != null) {
        val folder = state.selectedFolder!!
        val folderTreeVideos = remember(folder, state.videos) {
            state.videos.filter { it.path.startsWith(folder.path + "/") }
        }
        AlertDialog(
            onDismissRequest = { state.showFolderRenameDialog = false },
            title = { Text("重命名文件夹") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.folderRenameText,
                        onValueChange = { state.folderRenameText = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "将移动文件夹及其子文件夹中的 ${folderTreeVideos.size} 个视频到新目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { state.renameFolder(folder, state.folderRenameText.trim()) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showFolderRenameDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FolderDeleteDialog(state: VideoScreenState) {
    if (state.showFolderDeleteDialog && state.selectedFolder != null) {
        val folder = state.selectedFolder!!
        val folderTreeVideos = remember(folder, state.videos) {
            state.videos.filter { it.path.startsWith(folder.path + "/") }
        }
        AlertDialog(
            onDismissRequest = { state.showFolderDeleteDialog = false },
            title = { Text("删除文件夹") },
            text = { Text("确定要删除「${folder.name}」及子文件夹中的 ${folderTreeVideos.size} 个视频吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { state.deleteFolder(folder) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showFolderDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun VideoRenameDialog(state: VideoScreenState) {
    if (state.showRenameDialog && state.selectedVideo != null) {
        val video = state.selectedVideo!!
        AlertDialog(
            onDismissRequest = { state.showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = state.renameText,
                    onValueChange = { state.renameText = it },
                    label = { Text("新名称") },
                    placeholder = {
                        Text(
                            video.name.substringBeforeLast("."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.renameVideo(video, state.renameText.ifBlank { video.name.substringBeforeLast(".") })
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { state.showRenameDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun VideoDeleteDialog(state: VideoScreenState) {
    if (state.showDeleteDialog && state.selectedVideo != null) {
        val video = state.selectedVideo!!
        AlertDialog(
            onDismissRequest = { state.showDeleteDialog = false },
            title = { Text("删除视频") },
            text = { Text("确定要删除「${video.name}」吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { state.deleteVideo(video) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun VideoPropertiesDialog(state: VideoScreenState) {
    if (state.showPropertiesDialog && state.selectedVideo != null) {
        val video = state.selectedVideo!!
        AlertDialog(
            onDismissRequest = { state.showPropertiesDialog = false },
            title = { Text("视频属性") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("名称", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(video.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("时长", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(video.formattedDuration, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("大小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(formatFileSize(video.size), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { state.showPropertiesDialog = false }) { Text("关闭") }
            }
        )
    }
}

// ====================================================================
//  视频网格 / 列表组件
// ====================================================================

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
    LazyColumn(
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
            thumbnailBitmap = loadVideoThumbnail(context, video)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
                    modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = video.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                val meta = buildString {
                    append(video.formattedDuration)
                    if (video.size > 0) {
                        if (isNotEmpty()) append(" · ")
                        append(formatFileSize(video.size))
                    }
                    if (video.formattedResolution.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(video.formattedResolution)
                    }
                }
                Text(text = meta, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            thumbnailBitmap = loadVideoThumbnail(context, video)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box {
            if (thumbnailBitmap != null) {
                Image(
                    painter = BitmapPainter(thumbnailBitmap!!.asImageBitmap()),
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(text = video.formattedDuration, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(text = video.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            val meta = buildString {
                if (video.size > 0) append(formatFileSize(video.size))
                if (video.formattedResolution.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append(video.formattedResolution)
                }
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = meta, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun folderPainter(): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier("folder_thumb", "drawable", context.packageName)
    return painterResource(resId)
}