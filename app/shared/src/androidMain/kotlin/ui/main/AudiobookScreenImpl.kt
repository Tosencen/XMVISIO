package com.xmvisio.app.ui.main

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xmvisio.app.audio.AudioCategory
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.permissions.AudioPermissionManager
import com.xmvisio.app.permissions.PermissionStatus
import com.xmvisio.app.ui.audiobook.EmptyState
import com.xmvisio.app.ui.audiobook.formatTime
import com.xmvisio.app.ui.components.UpdateButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android 有声书页面实现（带权限管理和音频扫描）
 * 状态管理委托给 [AudiobookScreenState]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AudiobookScreenImpl(
    onNavigateToPlayer: (LocalAudioFile) -> Unit,
    modifier: Modifier = Modifier,
    updateAvailable: Boolean = false,
    onUpdateCheck: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ===== 状态持有者（ViewModel，跨组合存活；先创建，稍后绑定 intent launcher）=====
    val state: AudiobookScreenState = viewModel {
        AudiobookScreenState(context.applicationContext)
    }

    // ===== Intent launchers =====
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        state.onPermissionResult(isGranted)
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            state.scanAudioFiles(false)
        }
    }

    // 绑定 intent launcher 到 state（离开组合时清空，避免后台任务回调到失效的 launcher）
    DisposableEffect(Unit) {
        state.onLaunchIntent = { intentSenderLauncher.launch(it) }
        onDispose { state.onLaunchIntent = {} }
    }

    // ===== 生命周期监听 =====
    DisposableEffect(lifecycleOwner) {
        val observer = state.createLifecycleObserver()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ===== 媒体库变更监听（新添加的音频自动刷新，权限未授予时不注册）=====
    DisposableEffect(state.permissionStatus) {
        if (state.permissionStatus != PermissionStatus.GRANTED) return@DisposableEffect onDispose {}
        val observer = state.createAudioContentObserver()
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    // ===== 搜索框自动聚焦 =====
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.isSearching) {
        if (state.isSearching) { delay(100); focusRequester.requestFocus() }
    }

    // ===== 返回键处理 =====
    BackHandler(enabled = state.isSearching) { state.onBackInSearch() }
    BackHandler(enabled = state.isReorderMode) { state.onBackInReorder() }
    BackHandler(enabled = state.batchSelectionState.isActive) { state.onBackInBatchSelection() }
    BackHandler(enabled = state.isCategoryView) { state.onBackInCategoryView() }

    // ===== 全局播放器状态 =====
    val isPlaying by state.globalPlayer.isPlaying.collectAsState()
    val currentPosition by state.globalPlayer.currentPosition.collectAsState()
    val playerDuration by state.globalPlayer.duration.collectAsState()
    val currentPlayingAudioId by state.globalPlayer.currentAudioId.collectAsState()
    val controllerCurrentAudio by state.globalController.currentAudio.collectAsState()
    val controllerPlaylist by state.globalController.playlist.collectAsState()
    val recentAudioId by state.recentPlayManager.recentAudioId.collectAsState()
    val toastShowing by state.toastViewModel.showing.collectAsState()
    val toastContent by state.toastViewModel.content.collectAsState()

    // ===== 同步播放状态 =====
    LaunchedEffect(Unit) { state.globalPlayer.syncPlaybackState() }

    // ===== 主 UI =====
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    TopAppBar(
                        title = {
                            if (state.isSearching) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = { state.searchQuery = it },
                                    placeholder = { Text("搜索音频") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("有声")
                                    if (updateAvailable) {
                                        Spacer(Modifier.width(4.dp))
                                        UpdateButton(onClick = onUpdateCheck)
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (state.isSearching) {
                                IconButton(onClick = { state.isSearching = false; state.searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "关闭搜索")
                                }
                            }
                        },
                        actions = {
                            if (!state.isSearching && state.permissionStatus == PermissionStatus.GRANTED) {
                                val hasAudio = state.audioList.isNotEmpty()
                                val isInCategoryList = state.currentCategory != null && state.currentCategory != AudioCategory.ALL

                                // 批量选择按钮 — 只在分类音频列表内且有音频时显示
                                if (isInCategoryList && hasAudio) {
                                    IconButton(onClick = {
                                        if (state.batchSelectionState.isActive) {
                                            state.batchSelectionState = state.batchSelectionState.exitMode()
                                        } else {
                                            if (state.isReorderMode) state.isReorderMode = false
                                            state.batchSelectionState = state.batchSelectionState.copy(isActive = true)
                                        }
                                    }) {
                                        Icon(
                                            if (state.batchSelectionState.isActive) Icons.Default.Close else Icons.Default.Checklist,
                                            if (state.batchSelectionState.isActive) "取消选择" else "批量选择"
                                        )
                                    }
                                }
                                // 排序按钮 — 只在分类音频列表内且有音频时显示
                                if (isInCategoryList && hasAudio && !state.batchSelectionState.isActive) {
                                    IconButton(onClick = { state.isReorderMode = !state.isReorderMode }) {
                                        Icon(
                                            if (state.isReorderMode) Icons.Default.Check else Icons.Default.Sort,
                                            if (state.isReorderMode) "完成排序" else "排序"
                                        )
                                    }
                                }
                                // 搜索按钮 — 有音频时才显示
                                if (hasAudio) {
                                    IconButton(onClick = {
                                        if (state.batchSelectionState.isActive) state.batchSelectionState = state.batchSelectionState.exitMode()
                                        state.isSearching = true
                                    }) {
                                        Icon(Icons.Default.Search, "搜索")
                                    }
                                }
                                // 菜单按钮 — 始终显示
                                Box {
                                    IconButton(onClick = { state.showSettingsMenu = true }) {
                                        Icon(Icons.Default.MoreVert, "菜单")
                                    }
                                    DropdownMenu(
                                        expanded = state.showSettingsMenu,
                                        onDismissRequest = { state.showSettingsMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("创建新分类") },
                                            onClick = { state.showSettingsMenu = false; state.isAddCategoryFromSelection = false; state.showAddCategoryDialog = true },
                                            leadingIcon = { Icon(Icons.Default.Add, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("跳过时间") },
                                            onClick = { state.showSettingsMenu = false; state.showSeekTimeDialog = true },
                                            leadingIcon = { Icon(Icons.Default.FastForward, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("自动回退") },
                                            onClick = { state.showSettingsMenu = false; state.showAutoRewindDialog = true },
                                            leadingIcon = { Icon(Icons.Default.FastRewind, null) }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    // 分类面包屑
                    if (state.permissionStatus == PermissionStatus.GRANTED && !state.isSearching && state.currentCategory != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { state.currentCategory = null; state.selectedCategory = AudioCategory.ALL }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = state.currentCategory!!.name,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            },
            bottomBar = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (state.permissionStatus) {
                    PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            EmptyState(title = "需要存储权限", subtitle = "需要存储权限才能扫描本地的音频文件")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { activity?.let { permissionLauncher.launch(AudioPermissionManager.getRequiredPermission()) } }) {
                                Text("授予权限")
                            }
                        }
                    }
                    PermissionStatus.PERMANENTLY_DENIED -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            EmptyState(title = "权限被拒绝", subtitle = "要到系统设置里手动开一下权限")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { state.openAppSettings() }) { Text("打开设置") }
                        }
                    }
                    PermissionStatus.GRANTED -> {
                        if (state.isLoading && state.audioList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (state.audioList.isEmpty() && !state.isSearching) {
                            EmptyState(title = "还没有音频文件", subtitle = "手机里没找到音频文件", modifier = Modifier.fillMaxSize())
                        } else if (state.filteredAudioList.isEmpty() && state.searchQuery.isNotBlank()) {
                            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Search, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                Text("未找到结果", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\"${state.searchQuery}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        } else if (state.currentCategory == null) {
                            // 文件夹主页
                            // 计数缓存由 ViewModel 持有（版本变化才重查 DB），进入主页不再重复查询
                            LaunchedEffect(state.categories, state.categoryMappingVersion) {
                                state.ensureCategoryCountsLoaded()
                            }
                            AudioCategoryFolderGrid(
                                categories = state.categories,
                                audioList = state.filteredAudioList,
                                categoryCounts = state.categoryCounts,
                                isGridLayout = true,
                                onAllClick = { state.currentCategory = AudioCategory.ALL; state.selectedCategory = AudioCategory.ALL },
                                onCategoryClick = { category -> state.currentCategory = category; state.selectedCategory = category },
                                onCategoryLongClick = { category -> state.categoryToEdit = category; state.showCategoryManageDialog = true },
                                onCreateClick = { state.isAddCategoryFromSelection = false; state.showAddCategoryDialog = true }
                            )
                        } else {
                            // 分类音频列表
                            val category = state.currentCategory!!
                            val audioIdsForCategory by produceState(
                                initialValue = emptyList<Long>(),
                                key1 = category.id, key2 = state.categoryMappingVersion
                            ) {
                                value = if (category.id == AudioCategory.ALL.id) emptyList()
                                else state.categoryManager.getAudioIdsByCategory(category.id)
                            }

                            val baseCategoryFilteredList = remember(state.filteredAudioList, audioIdsForCategory, category) {
                                if (category.id == AudioCategory.ALL.id) state.filteredAudioList
                                else state.filteredAudioList.filter { it.id in audioIdsForCategory }
                            }

                            // 用 derivedStateOf 而非 remember 捕获：cache 是 mutableStateMapOf（快照状态），
                            // derivedStateOf 在内部读取它会建立依赖，reorderInCategory / updateCategorySortedList
                            // 写入 cache 后列表自动重组，排序模式不会“弹回原位”。
                            val categoryFilteredList by remember(baseCategoryFilteredList, category.id) {
                                derivedStateOf {
                                    state.getCategorySortedList(category.id, baseCategoryFilteredList)
                                }
                            }

                            LaunchedEffect(baseCategoryFilteredList) { state.updateCategorySortedList(category.id, baseCategoryFilteredList) }
                            LaunchedEffect(categoryFilteredList) { state.currentPageAudioList = categoryFilteredList }

                            val listState = rememberLazyListState()
                            val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
                                if (state.isReorderMode) state.reorderInCategory(category.id, from.index, to.index)
                            }

                            PullToRefreshBox(
                                isRefreshing = state.isRefreshing,
                                onRefresh = { state.scanAudioFiles(true) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(items = categoryFilteredList, key = { it.id }) { audio ->
                                        ReorderableItem(reorderableLazyListState, key = audio.id) { isDragging ->
                                            // 播放位置直接从 ViewModel 缓存读（扫描时一次加载），不再每个列表项异步查 prefs
                                            val isThisAudioPlaying = isPlaying && currentPlayingAudioId == audio.id
                                            val savedPosition = if (isThisAudioPlaying) {
                                                currentPosition
                                            } else {
                                                state.getSavedPosition(audio.id)
                                            }
                                            AudioItem(
                                                audio = audio,
                                                savedPosition = savedPosition,
                                                isThisAudioPlaying = isThisAudioPlaying,
                                                isReorderMode = state.isReorderMode,
                                                isDragging = isDragging,
                                                dragHandleModifier = Modifier.draggableHandle(),
                                                isBatchSelectionMode = state.batchSelectionState.isActive,
                                                isSelected = state.batchSelectionState.isSelected(audio.id),
                                                onCardClick = {
                                                    if (state.batchSelectionState.isActive) {
                                                        state.batchSelectionState = state.batchSelectionState.toggleSelection(audio.id)
                                                    } else if (!state.isReorderMode) {
                                                        state.playAudio(audio); onNavigateToPlayer(audio)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!state.isReorderMode && !state.batchSelectionState.isActive) {
                                                        state.selectedAudio = audio; state.showContextMenu = true
                                                    }
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                    item {
                                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                            Text("— 用心聆听，感受声音的力量 —", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 批量选择底部面板
                if (state.batchSelectionState.hasSelection && state.permissionStatus == PermissionStatus.GRANTED) {
                    Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).zIndex(1000f)) {
                        com.xmvisio.app.ui.audiobook.BatchSelectionBottomPanel(
                            visible = state.batchSelectionState.hasSelection,
                            selectedCount = state.batchSelectionState.selectionCount,
                            categories = state.categories.filter { it.id != AudioCategory.ALL.id },
                            onCategoryClick = { category -> state.batchSetCategory(category) },
                            onDeleteClick = { state.showBatchDeleteDialog = true },
                            onClose = { state.batchSelectionState = state.batchSelectionState.exitMode() }
                        )
                    }
                }

                // MiniPlayerBar
                val displayAudioId = currentPlayingAudioId ?: recentAudioId
                val playingAudio = controllerCurrentAudio ?: state.audioList.find { it.id == displayAudioId }
                if (playingAudio != null && !state.isSearching && !state.isReorderMode && !state.batchSelectionState.isActive && state.permissionStatus == PermissionStatus.GRANTED) {
                    val currentIndex = controllerPlaylist.indexOfFirst { it.id == currentPlayingAudioId }
                    val canSkipNext = currentIndex >= 0 && currentIndex < controllerPlaylist.size - 1
                    val canSkipPrevious = currentIndex > 0
                    Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).zIndex(999f)) {
                        com.xmvisio.app.ui.player.MiniPlayerBar(
                            audio = playingAudio, isPlaying = isPlaying, position = currentPosition, duration = playerDuration,
                            canSkipNext = canSkipNext, canSkipPrevious = canSkipPrevious,
                            onPlayPauseClick = {
                                if (currentPlayingAudioId == null) {
                                    scope.launch { state.globalPlayer.prepare(uri = playingAudio.uri, audioId = playingAudio.id, onPrepared = { state.globalPlayer.play() }) }
                                } else { state.globalPlayer.togglePlayPause() }
                            },
                            onNextClick = { state.globalController.playNext() },
                            onPreviousClick = { state.globalController.playPrevious() },
                            onClick = { onNavigateToPlayer(playingAudio) }
                        )
                    }
                }
            }
        }

        // ===== 上下文菜单 =====
        if (state.showContextMenu && state.selectedAudio != null) {
            var currentCategoryName by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(state.selectedAudio) {
                val categoryId = state.categoryManager.getAudioCategory(state.selectedAudio!!.id)
                currentCategoryName = if (categoryId != null) state.categories.find { it.id == categoryId }?.name else null
            }
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { state.showContextMenu = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(state.selectedAudio!!.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Surface(onClick = { state.showCategoryDialog = true; state.showContextMenu = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.onSurface)
                            Text("添加分类", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Surface(onClick = { state.renameText = state.selectedAudio!!.title; state.showRenameDialog = true; state.showContextMenu = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DriveFileRenameOutline, null, tint = MaterialTheme.colorScheme.onSurface)
                            Text("重命名", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Surface(onClick = { state.showPropertiesDialog = true; state.showContextMenu = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurface)
                            Text("属性", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Surface(onClick = { state.showDeleteDialog = true; state.showContextMenu = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Text("删除", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // ===== 属性对话框 =====
        if (state.showPropertiesDialog && state.selectedAudio != null) {
            com.xmvisio.app.ui.audiobook.AudioPropertiesDialog(audio = state.selectedAudio!!, onDismiss = { state.showPropertiesDialog = false })
        }

        // ===== 重命名对话框 =====
        if (state.showRenameDialog && state.selectedAudio != null) {
            AlertDialog(
                onDismissRequest = { state.showRenameDialog = false },
                title = { Text("重命名") },
                text = { OutlinedTextField(value = state.renameText, onValueChange = { state.renameText = it }, label = { Text("新名称") }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        val extension = state.selectedAudio!!.displayName.substringAfterLast(".", "")
                        val finalName = if (extension.isNotEmpty()) "${state.renameText}.$extension" else state.renameText
                        state.renameAudio(state.selectedAudio!!.uri, finalName)
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { state.showRenameDialog = false }) { Text("取消") } }
            )
        }

        // ===== 删除确认对话框 =====
        if (state.showDeleteDialog && state.selectedAudio != null) {
            AlertDialog(
                onDismissRequest = { state.showDeleteDialog = false },
                title = { Text("删除音频") },
                text = { Text("确定要删除 \"${state.selectedAudio!!.title}\" 吗？") },
                confirmButton = { TextButton(onClick = { state.deleteAudio(state.selectedAudio!!.uri) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { state.showDeleteDialog = false }) { Text("取消") } }
            )
        }

        // ===== 分类选择对话框 =====
        if (state.showCategoryDialog && state.selectedAudio != null) {
            var selectedCategoryId by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(state.selectedAudio) { selectedCategoryId = state.categoryManager.getAudioCategory(state.selectedAudio!!.id) }
            AlertDialog(
                onDismissRequest = { state.showCategoryDialog = false },
                title = { Text("选择分类") },
                text = {
                    LazyColumn {
                        item {
                            Row(Modifier.fillMaxWidth().clickable { selectedCategoryId = null }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedCategoryId == null, onClick = null); Spacer(Modifier.width(8.dp)); Text("无分类")
                            }
                        }
                        items(state.categories.filter { it.id != AudioCategory.ALL.id }) { category ->
                            Row(Modifier.fillMaxWidth().clickable { selectedCategoryId = category.id }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = category.id == selectedCategoryId, onClick = null); Spacer(Modifier.width(8.dp)); Text(category.name)
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { state.isAddCategoryFromSelection = true; state.showAddCategoryDialog = true; state.showCategoryDialog = false },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("新增分类")
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        if (selectedCategoryId != null && state.categories.any { it.id == selectedCategoryId }) {
                            TextButton(onClick = { state.categoryToDelete = state.categories.find { it.id == selectedCategoryId }; state.showDeleteCategoryDialog = true; state.showCategoryDialog = false }) {
                                Text("删除分类", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { state.showCategoryDialog = false }) { Text("取消") }
                        TextButton(onClick = { state.setAudioCategory(state.selectedAudio!!.id, selectedCategoryId) }) { Text("确定") }
                    }
                },
                dismissButton = {}
            )
        }

        // ===== 新增分类对话框 =====
        if (state.showAddCategoryDialog) {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { state.showAddCategoryDialog = false; state.newCategoryName = ""; errorMessage = null },
                title = { Text("新增分类") },
                text = {
                    OutlinedTextField(value = state.newCategoryName, onValueChange = { state.newCategoryName = it; errorMessage = null },
                        label = { Text("分类名称") }, singleLine = true, isError = errorMessage != null,
                        supportingText = if (errorMessage != null) {{ Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }} else null)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (state.newCategoryName.isNotBlank()) {
                                if (state.categories.any { it.name.equals(state.newCategoryName.trim(), ignoreCase = true) }) {
                                    errorMessage = "分类名称已存在"
                                } else { state.addCategory(state.newCategoryName.trim()) }
                            }
                        },
                        enabled = state.newCategoryName.isNotBlank()
                    ) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { state.showAddCategoryDialog = false; state.newCategoryName = ""; errorMessage = null }) { Text("取消") } }
            )
        }

        // ===== 删除分类对话框 =====
        if (state.showDeleteCategoryDialog && state.categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { state.showDeleteCategoryDialog = false; state.categoryToDelete = null },
                title = { Text("删除分类") },
                text = { Text("确定要删除分类 \"${state.categoryToDelete!!.name}\" 吗？该分类下的音频将变为未分类。") },
                confirmButton = { TextButton(onClick = { state.deleteCategory(state.categoryToDelete!!.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { state.showDeleteCategoryDialog = false; state.categoryToDelete = null }) { Text("取消") } }
            )
        }

        // ===== 分类管理底部弹窗 =====
        if (state.showCategoryManageDialog && state.categoryToEdit != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { state.showCategoryManageDialog = false; state.categoryToEdit = null },
                sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(state.categoryToEdit!!.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(onClick = { state.renameText = state.categoryToEdit!!.name; state.showRenameCategoryDialog = true; state.showCategoryManageDialog = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DriveFileRenameOutline, null, tint = MaterialTheme.colorScheme.onSurface); Text("重命名", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Surface(onClick = { state.categoryToDelete = state.categoryToEdit; state.showDeleteCategoryDialog = true; state.showCategoryManageDialog = false }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error); Text("删除", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // ===== 重命名分类对话框 =====
        if (state.showRenameCategoryDialog && state.categoryToEdit != null) {
            AlertDialog(
                onDismissRequest = { state.showRenameCategoryDialog = false; state.categoryToEdit = null },
                title = { Text("重命名分类") },
                text = { OutlinedTextField(value = state.renameText, onValueChange = { state.renameText = it }, label = { Text("新名称") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { if (state.renameText.isNotBlank()) state.renameCategory(state.categoryToEdit!!.id, state.renameText) }, enabled = state.renameText.isNotBlank()) { Text("确定") } },
                dismissButton = { TextButton(onClick = { state.showRenameCategoryDialog = false; state.categoryToEdit = null; state.renameText = "" }) { Text("取消") } }
            )
        }

        // ===== 跳过时间设置对话框 =====
        if (state.showSeekTimeDialog) {
            var selectedSeconds by remember { mutableIntStateOf(state.seekTimeInSeconds) }
            AlertDialog(
                onDismissRequest = { state.showSeekTimeDialog = false },
                title = { Text("跳过时间") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("设置快进/快退的时间间隔", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Column {
                            Slider(value = selectedSeconds.toFloat(), onValueChange = { selectedSeconds = it.toInt() }, valueRange = 3f..60f, steps = 56, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("3秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${selectedSeconds}秒", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text("60秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { state.saveSeekTime(selectedSeconds) }) { Text("确定") } },
                dismissButton = { TextButton(onClick = { state.showSeekTimeDialog = false }) { Text("取消") } }
            )
        }

        // ===== 自动回退设置对话框 =====
        if (state.showAutoRewindDialog) {
            var selectedSeconds by remember { mutableIntStateOf(state.autoRewindInSeconds) }
            AlertDialog(
                onDismissRequest = { state.showAutoRewindDialog = false },
                title = { Text("自动回退") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("暂停后恢复播放时自动回退的秒数", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Column {
                            Slider(value = selectedSeconds.toFloat(), onValueChange = { selectedSeconds = it.toInt() }, valueRange = 0f..20f, steps = 19, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("0秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${selectedSeconds}秒", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text("20秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { state.saveAutoRewind(selectedSeconds) }) { Text("确定") } },
                dismissButton = { TextButton(onClick = { state.showAutoRewindDialog = false }) { Text("取消") } }
            )
        }

        // ===== 批量删除确认对话框 =====
        if (state.showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { state.showBatchDeleteDialog = false },
                title = { Text("批量删除") },
                text = { Text("确定要删除选中的 ${state.batchSelectionState.selectionCount} 个音频吗？此操作无法撤销。") },
                confirmButton = { TextButton(onClick = { state.batchDelete() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { state.showBatchDeleteDialog = false }) { Text("取消") } }
            )
        }

        // Toast
        com.xmvisio.app.ui.components.ToastContent(
            showing = { toastShowing },
            content = { Text(toastContent) }
        )
    }
}

// ====================================================================
//  Helper composables（保持不变）
// ====================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioCategoryFolderGrid(
    categories: List<AudioCategory>,
    audioList: List<LocalAudioFile>,
    categoryCounts: Map<String, Int>,
    isGridLayout: Boolean,
    onAllClick: () -> Unit,
    onCategoryClick: (AudioCategory) -> Unit,
    onCategoryLongClick: (AudioCategory) -> Unit,
    onCreateClick: () -> Unit
) {
    // 计数由 ViewModel 缓存提供（见 state.ensureCategoryCountsLoaded），避免每次进入都查 DB
    if (isGridLayout) {
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            item { AudioCategoryFolderCard(name = "全部音频", count = audioList.size, isAll = true, onClick = onAllClick, onLongClick = null) }
            gridItems(categories.filter { it.id != AudioCategory.ALL.id }, key = { it.id }) { category ->
                AudioCategoryFolderCard(name = category.name, count = categoryCounts[category.id] ?: 0, isAll = false, onClick = { onCategoryClick(category) }, onLongClick = { onCategoryLongClick(category) })
            }
            item { AudioCategoryFolderCard(name = "新建分类", count = -1, isAll = false, onClick = onCreateClick, onLongClick = null) }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            item { AudioCategoryFolderRow(name = "全部音频", count = audioList.size, onClick = onAllClick) }
            items(categories.filter { it.id != AudioCategory.ALL.id }, key = { it.id }) { category ->
                AudioCategoryFolderRow(name = category.name, count = categoryCounts[category.id] ?: 0, onClick = { onCategoryClick(category) })
            }
            item { AudioCategoryFolderRow(name = "新建分类", count = -1, onClick = onCreateClick) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioCategoryFolderCard(name: String, count: Int, isAll: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(painter = folderPainterForAudio(), contentDescription = null,
                tint = if (count < 0) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.width(90.dp).aspectRatio(20 / 17f))
            if (isAll) Text("全部", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
            if (count < 0) Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).align(Alignment.Center).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(4.dp))
        }
        Text(name, maxLines = 1, style = MaterialTheme.typography.titleMedium, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        if (count >= 0) Text("$count 个音频", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudioCategoryFolderRow(name: String, count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painter = folderPainterForAudio(), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.surfaceContainerHigh)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (count >= 0) Text("$count 个音频", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (count < 0) Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun folderPainterForAudio(): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier("folder_thumb", "drawable", context.packageName)
    return painterResource(resId)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AudioItem(
    audio: LocalAudioFile,
    savedPosition: kotlin.time.Duration,
    isThisAudioPlaying: Boolean,
    isReorderMode: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBatchSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val duration = remember(audio.duration) { audio.duration.milliseconds }
    val progress = remember(savedPosition, duration) { if (duration.inWholeMilliseconds > 0) (savedPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f) else 0f }
    val remainingTime = remember(savedPosition, duration) {
        val remaining = duration - savedPosition; val totalSeconds = remaining.inWholeSeconds; val hours = totalSeconds / 3600; val minutes = (totalSeconds % 3600) / 60
        when { hours > 0 -> "${hours}小时${minutes}分钟"; minutes > 0 -> "${minutes}分钟"; else -> "不到1分钟" }
    }
    ElevatedCard(
        modifier = modifier.fillMaxWidth().combinedClickable(onClick = onCardClick, onLongClick = if (isBatchSelectionMode) null else onLongClick, enabled = !isReorderMode),
        colors = CardDefaults.elevatedCardColors(containerColor = if (isSelected && isBatchSelectionMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp, draggedElevation = if (isDragging) 4.dp else 0.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.animation.AnimatedVisibility(visible = isBatchSelectionMode, enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(), exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
                    Row { Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.padding(end = 8.dp)) }
                }
                Box(Modifier.size(56.dp)) {
                    Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                    if (isThisAudioPlaying) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                            com.xmvisio.app.ui.foundation.PlayingAnimation(Modifier.size(28.dp, 20.dp), color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(audio.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (isThisAudioPlaying) Modifier.basicMarquee() else Modifier)
                    if (audio.artist != null) Text(audio.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (isThisAudioPlaying) Modifier.basicMarquee() else Modifier)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (progress > 0f) remainingTime else formatTime(audio.duration / 1000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (progress > 0f) Text("已播${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isReorderMode) {
                    // dragHandleModifier 必须应用到拖拽手柄上（draggableHandle() 内含长按拖拽手势），
                    // 否则排序模式下无法拖动（曾因漏挂导致无法调整音频位置）。
                    // testTag 供 instrumented 测试定位真实拖拽手柄。
                    Box(
                        Modifier.size(48.dp).testTag("drag_handle_${audio.id}").then(dragHandleModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DragHandle, "拖动排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}