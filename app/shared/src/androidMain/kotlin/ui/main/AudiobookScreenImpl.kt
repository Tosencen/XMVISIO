package com.xmvisio.app.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.geometry.Offset
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xmvisio.app.audio.AudioCategory
import com.xmvisio.app.audio.AudioManager
import com.xmvisio.app.audio.AudioOperationResult
import com.xmvisio.app.audio.AudioScanner
import com.xmvisio.app.audio.CategoryManager
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.audio.RecentPlayManager
import com.xmvisio.app.ui.foundation.pagerTabIndicatorOffset
import com.xmvisio.app.permissions.AudioPermissionManager
import com.xmvisio.app.permissions.PermissionStatus
import com.xmvisio.app.ui.audiobook.EmptyState
import com.xmvisio.app.ui.audiobook.formatTime
import com.xmvisio.app.ui.player.AudioPlayerScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android 有声书页面实现（带权限管理和音频扫描）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AudiobookScreenImpl(
    onNavigateToPlayer: (LocalAudioFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 权限管理
    val permissionManager = remember { AudioPermissionManager(context) }
    var permissionStatus by remember { mutableStateOf(permissionManager.checkPermissionStatus()) }
    
    // 音频扫描和管理
    val audioScanner = remember { AudioScanner(context) }
    val audioManager = remember { com.xmvisio.app.audio.GlobalAudioManager.getInstance(context) }
    val categoryManager = remember { CategoryManager(context) }
    
    // 状态
    var audioList by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 分类状态
    var categories by remember { mutableStateOf<List<AudioCategory>>(listOf(AudioCategory.ALL)) }
    var selectedCategory by remember { mutableStateOf(AudioCategory.ALL) }
    
    // 搜索状态
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // 排序模式状态
    var isReorderMode by remember { mutableStateOf(false) }
    val audioOrderManager = remember { com.xmvisio.app.audio.AudioOrderManager(context) }
    
    // 存储每个分类的排序列表 - 避免页面切换时重新加载
    val categorySortedListsCache = remember { mutableStateMapOf<String, List<LocalAudioFile>>() }
    
    // Pager状态 - 用于左右滑动切换分类
    val initialPage = remember(categories) {
        categories.indexOfFirst { it.id == selectedCategory.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { categories.size }
    )
    
    // 同步PagerState和selectedCategory
    LaunchedEffect(pagerState.currentPage) {
        val newCategory = categories.getOrNull(pagerState.currentPage)
        if (newCategory != null && newCategory.id != selectedCategory.id) {
            selectedCategory = newCategory
        }
    }
    
    // 同步selectedCategory和PagerState
    LaunchedEffect(selectedCategory) {
        val targetIndex = categories.indexOfFirst { it.id == selectedCategory.id }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }
    
    // 过滤后的音频列表（按搜索）
    val filteredAudioList = remember(audioList, searchQuery) {
        if (searchQuery.isNotBlank()) {
            audioList.filter { audio ->
                audio.title.contains(searchQuery, ignoreCase = true) ||
                audio.artist?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            audioList
        }
    }
    
    // 当前页面的排序后音频列表（用于 MiniPlayerBar 的上一首/下一首）
    var currentPageAudioList by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    
    // 菜单状态
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedAudio by remember { mutableStateOf<LocalAudioFile?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isAddCategoryFromSelection by remember { mutableStateOf(false) }  // 标记是否从分类选择对话框触发
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var showRenameCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<AudioCategory?>(null) }
    var categoryToEdit by remember { mutableStateOf<AudioCategory?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }
    var showCategoryManageDialog by remember { mutableStateOf(false) }
    var categoryMappingVersion by remember { mutableStateOf(0) }  // 用于触发分类映射刷新
    
    // 设置菜单状态
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSeekTimeDialog by remember { mutableStateOf(false) }
    var showAutoRewindDialog by remember { mutableStateOf(false) }
    
    // 播放设置（使用SharedPreferences保存）
    val prefs = remember { context.getSharedPreferences("audio_settings", android.content.Context.MODE_PRIVATE) }
    var seekTimeInSeconds by remember { mutableStateOf(prefs.getInt("seek_time", 10)) }
    var autoRewindInSeconds by remember { mutableStateOf(prefs.getInt("auto_rewind", 2)) }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionStatus = if (isGranted) {
            PermissionStatus.GRANTED
        } else {
            permissionManager.checkPermissionStatus()
        }
        
        if (isGranted) {
            scope.launch {
                isLoading = true
                audioList = audioScanner.scanAudioFiles()
                isLoading = false
            }
        }
    }
    
    // 扫描音频文件和加载分类
    val scanAudioFiles: (Boolean) -> Unit = { isRefresh ->
        if (isRefresh) {
            isRefreshing = true
        } else {
            isLoading = true
        }
        
        scope.launch {
            if (isRefresh) delay(300)
            audioList = audioScanner.scanAudioFiles()
            categories = categoryManager.getCategories()
            isRefreshing = false
            isLoading = false
        }
    }
    
    // 媒体修改权限启动器（Android 10+）
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 用户授权，重新执行操作
            scope.launch {
                scanAudioFiles(false)
            }
        }
    }
    
    // 监听生命周期，返回时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = permissionManager.checkPermissionStatus()
                if (permissionStatus == PermissionStatus.GRANTED && audioList.isEmpty()) {
                    scanAudioFiles(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 搜索框自动聚焦
    LaunchedEffect(isSearching) {
        if (isSearching) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
    
    // 返回键处理：在搜索状态下按返回键直接关闭搜索
    BackHandler(enabled = isSearching) {
        isSearching = false
        searchQuery = ""
    }
    
    // 返回键处理：在排序模式下按返回键退出排序模式
    BackHandler(enabled = isReorderMode) {
        isReorderMode = false
    }
    
    // 最近播放管理器
    val recentPlayManager = remember { RecentPlayManager.getInstance(context) }
    val recentAudioId by recentPlayManager.recentAudioId.collectAsState()
    
    // Snackbar 状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("搜索音频") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            Text("有声")
                        }
                    },
                    navigationIcon = {
                        if (isSearching) {
                            IconButton(onClick = {
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, "关闭搜索")
                            }
                        }
                    },
                    actions = {
                        if (!isSearching && permissionStatus == PermissionStatus.GRANTED) {
                            // 排序按钮
                            IconButton(
                                onClick = { isReorderMode = !isReorderMode }
                            ) {
                                Icon(
                                    if (isReorderMode) Icons.Default.Check else Icons.Default.Sort,
                                    if (isReorderMode) "完成排序" else "排序"
                                )
                            }
                            
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, "搜索")
                            }
                            
                            // 菜单按钮
                            Box {
                                IconButton(onClick = { showSettingsMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "菜单")
                                }
                                
                                DropdownMenu(
                                    expanded = showSettingsMenu,
                                    onDismissRequest = { showSettingsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("创建新分类") },
                                        onClick = {
                                            showSettingsMenu = false
                                            isAddCategoryFromSelection = false  // 从菜单触发
                                            showAddCategoryDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("跳过时间") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showSeekTimeDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.FastForward, null)
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("自动回退") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showAutoRewindDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.FastRewind, null)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
                
                // 分类标签行 - 一比一复刻XMSLEEP的Tab样式
                if (permissionStatus == PermissionStatus.GRANTED && !isSearching) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (categories.isNotEmpty()) {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                edgePadding = 0.dp,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                                    )
                                },
                                divider = { }  // 不显示TabRow自带的divider
                            ) {
                                categories.forEachIndexed { index, category ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch { 
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = {
                                            Box(
                                                modifier = Modifier.combinedClickable(
                                                    onClick = {
                                                        scope.launch { 
                                                            pagerState.animateScrollToPage(index)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        // 只有非"全部"分类才能长按编辑
                                                        if (category.id != AudioCategory.ALL.id) {
                                                            categoryToEdit = category
                                                            showCategoryManageDialog = true
                                                        }
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 始终显示的分隔线（拉通整个页面）
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        },
        bottomBar = {},
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (permissionStatus) {
                PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
                    // 请求权限
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(
                            title = "需要存储权限",
                            subtitle = "请授予存储权限以扫描和导入音频文件"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                activity?.let {
                                    permissionLauncher.launch(
                                        AudioPermissionManager.getRequiredPermission()
                                    )
                                }
                            }
                        ) {
                            Text("授予权限")
                        }
                    }
                }
                
                PermissionStatus.PERMANENTLY_DENIED -> {
                    // 永久拒绝，引导去设置
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(
                            title = "权限被拒绝",
                            subtitle = "请在系统设置中手动授予存储权限"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("打开设置")
                        }
                    }
                }
                
                PermissionStatus.GRANTED -> {
                    // 已授权，显示音频列表
                    if (isLoading && audioList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (audioList.isEmpty() && !isSearching) {
                        EmptyState(
                            title = "还没有音频文件",
                            subtitle = "设备上没有找到音频文件"
                        )
                    } else if (filteredAudioList.isEmpty() && searchQuery.isNotBlank()) {
                        // 搜索无结果
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "未找到结果",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        // 音频列表 - 使用HorizontalPager实现左右滑动
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            pageSpacing = 0.dp
                        ) { pageIndex ->
                            val category = categories[pageIndex]
                            
                            // 获取该分类下的音频ID列表
                            val audioIdsForCategory by produceState(
                                initialValue = emptyList<Long>(),
                                key1 = category.id,
                                key2 = categoryMappingVersion  // 监听映射版本变化，触发刷新
                            ) {
                                value = if (category.id == AudioCategory.ALL.id) {
                                    emptyList()
                                } else {
                                    categoryManager.getAudioIdsByCategory(category.id)
                                }
                            }
                            
                            // 按分类过滤音频
                            val baseCategoryFilteredList = remember(filteredAudioList, audioIdsForCategory, category) {
                                if (category.id == AudioCategory.ALL.id) {
                                    filteredAudioList
                                } else {
                                    filteredAudioList.filter { it.id in audioIdsForCategory }
                                }
                            }
                            
                            // 从缓存获取或加载排序列表（初始化时就应用排序）
                            val categoryFilteredList = categorySortedListsCache.getOrPut(category.id) {
                                // 同步读取自定义排序
                                val customOrder = audioOrderManager.getOrderSync(
                                    if (category.id == AudioCategory.ALL.id) null else category.id
                                )
                                
                                if (customOrder != null) {
                                    // 按照自定义排序重新排列
                                    val orderedList = mutableListOf<LocalAudioFile>()
                                    customOrder.forEach { audioId ->
                                        baseCategoryFilteredList.find { it.id == audioId }?.let { orderedList.add(it) }
                                    }
                                    // 添加不在自定义排序中的新音频
                                    baseCategoryFilteredList.forEach { audio ->
                                        if (audio.id !in customOrder) {
                                            orderedList.add(audio)
                                        }
                                    }
                                    orderedList
                                } else {
                                    baseCategoryFilteredList
                                }
                            }
                            
                            // 当 baseCategoryFilteredList 变化时更新缓存
                            LaunchedEffect(baseCategoryFilteredList) {
                                val customOrder = audioOrderManager.getOrderSync(
                                    if (category.id == AudioCategory.ALL.id) null else category.id
                                )
                                
                                val sortedList = if (customOrder != null) {
                                    val orderedList = mutableListOf<LocalAudioFile>()
                                    customOrder.forEach { audioId ->
                                        baseCategoryFilteredList.find { it.id == audioId }?.let { orderedList.add(it) }
                                    }
                                    baseCategoryFilteredList.forEach { audio ->
                                        if (audio.id !in customOrder) {
                                            orderedList.add(audio)
                                        }
                                    }
                                    orderedList
                                } else {
                                    baseCategoryFilteredList
                                }
                                
                                categorySortedListsCache[category.id] = sortedList
                            }
                            
                            // 更新当前页面的音频列表（用于 MiniPlayerBar）
                            LaunchedEffect(categoryFilteredList, pagerState.currentPage, pageIndex) {
                                if (pagerState.currentPage == pageIndex) {
                                    currentPageAudioList = categoryFilteredList
                                }
                            }
                            
                            // 滚动状态
                            val listState = rememberLazyListState()
                            
                            // Reorderable 状态
                            val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
                                if (isReorderMode) {
                                    val fromIndex = from.index
                                    val toIndex = to.index
                                    
                                    val newList = categoryFilteredList.toMutableList()
                                    val item = newList.removeAt(fromIndex)
                                    newList.add(toIndex, item)
                                    
                                    // 更新缓存
                                    categorySortedListsCache[category.id] = newList
                                    
                                    // 保存排序
                                    scope.launch {
                                        audioOrderManager.saveOrder(
                                            if (category.id == AudioCategory.ALL.id) null else category.id,
                                            newList.map { it.id }
                                        )
                                    }
                                }
                            }
                            
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { scanAudioFiles(true) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 120.dp  // 增加底部间距，避免被悬浮条遮挡
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = categoryFilteredList,
                                        key = { it.id }
                                    ) { audio ->
                                        ReorderableItem(
                                            reorderableLazyListState,
                                            key = audio.id
                                        ) { isDragging ->
                                            AudioItem(
                                                audio = audio,
                                                isReorderMode = isReorderMode,
                                                isDragging = isDragging,
                                                dragHandleModifier = Modifier.draggableHandle(),
                                                onCardClick = {
                                                    if (!isReorderMode) {
                                                        // 记录最近播放
                                                        scope.launch {
                                                            recentPlayManager.recordRecentPlay(audio.id, audio.title)
                                                        }
                                                        onNavigateToPlayer(audio)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isReorderMode) {
                                                        selectedAudio = audio
                                                        showContextMenu = true
                                                    }
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                    
                                    // 底部标语
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "— 用心聆听，感受声音的力量 —",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 悬浮的 MiniPlayerBar
            val globalPlayer = remember { com.xmvisio.app.audio.GlobalAudioPlayer.getInstance(context) }
            val globalController = remember { com.xmvisio.app.audio.GlobalAudioPlayerController.getInstance(context) }
            
            // 同步播放状态（应用恢复时）
            LaunchedEffect(Unit) {
                globalPlayer.syncPlaybackState()
            }
            
            val isPlaying by globalPlayer.isPlaying.collectAsState()
            val currentPosition by globalPlayer.currentPosition.collectAsState()
            val playerDuration by globalPlayer.duration.collectAsState()
            val currentPlayingAudioId by globalPlayer.currentAudioId.collectAsState()
            
            // 从 GlobalAudioPlayerController 获取当前播放的音频（即使不在当前分类列表中也能显示）
            val controllerCurrentAudio by globalController.currentAudio.collectAsState()
            
            // 显示 MiniPlayerBar：优先使用 GlobalAudioPlayerController 的 currentAudio，
            // 如果没有则尝试从 audioList 中查找最近播放的音频
            val displayAudioId = currentPlayingAudioId ?: recentAudioId
            val playingAudio = controllerCurrentAudio ?: audioList.find { it.id == displayAudioId }
            
            if (playingAudio != null && !isSearching && !isReorderMode && permissionStatus == PermissionStatus.GRANTED) {
                // 使用 GlobalAudioPlayerController 的播放列表来判断上一首/下一首
                val controllerPlaylist by globalController.playlist.collectAsState()
                val currentIndex = controllerPlaylist.indexOfFirst { it.id == currentPlayingAudioId }
                val canSkipNext = currentIndex >= 0 && currentIndex < controllerPlaylist.size - 1
                val canSkipPrevious = currentIndex > 0
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .zIndex(999f)
                    ) {
                        com.xmvisio.app.ui.player.MiniPlayerBar(
                            audio = playingAudio,
                            isPlaying = isPlaying,
                            position = currentPosition,
                            duration = playerDuration,
                            canSkipNext = canSkipNext,
                            canSkipPrevious = canSkipPrevious,
                            onPlayPauseClick = { 
                                // 如果当前没有播放音频（只是显示最近播放），需要先准备
                                if (currentPlayingAudioId == null) {
                                    scope.launch {
                                        globalPlayer.prepare(
                                            uri = playingAudio.uri,
                                            audioId = playingAudio.id,
                                            onPrepared = { globalPlayer.play() }
                                        )
                                    }
                                } else {
                                    globalPlayer.togglePlayPause()
                                }
                            },
                            onNextClick = {
                                // 使用 GlobalAudioPlayerController 来播放下一首
                                globalController.playNext()
                            },
                            onPreviousClick = {
                                // 使用 GlobalAudioPlayerController 来播放上一首
                                globalController.playPrevious()
                            },
                            onFavoriteClick = {},
                            onClick = { onNavigateToPlayer(playingAudio) },
                            isFavorite = false
                        )
                    }
            }
        }
    }
    
    // 上下文菜单
    if (showContextMenu && selectedAudio != null) {
        var currentCategoryName by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(selectedAudio) {
            val categoryId = categoryManager.getAudioCategory(selectedAudio!!.id)
            currentCategoryName = if (categoryId != null) {
                categories.find { it.id == categoryId }?.name
            } else {
                null
            }
        }
        
        val sheetState = rememberModalBottomSheetState()
        
        ModalBottomSheet(
            onDismissRequest = { showContextMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 标题
                Text(
                    text = selectedAudio!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 添加分类选项
                Surface(
                    onClick = {
                        showCategoryDialog = true
                        showContextMenu = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "添加分类",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // 重命名选项
                Surface(
                    onClick = {
                        renameText = selectedAudio!!.title
                        showRenameDialog = true
                        showContextMenu = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "重命名",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // 属性选项
                Surface(
                    onClick = {
                        showPropertiesDialog = true
                        showContextMenu = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "属性",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // 删除选项
                Surface(
                    onClick = {
                        showDeleteDialog = true
                        showContextMenu = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // 属性对话框
    if (showPropertiesDialog && selectedAudio != null) {
        com.xmvisio.app.ui.audiobook.AudioPropertiesDialog(
            audio = selectedAudio!!,
            onDismiss = { showPropertiesDialog = false }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog && selectedAudio != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val extension = selectedAudio!!.displayName.substringAfterLast(".", "")
                            val finalName = if (extension.isNotEmpty()) {
                                "$renameText.$extension"
                            } else {
                                renameText
                            }
                            
                            when (val result = audioManager.renameAudio(selectedAudio!!.uri, finalName)) {
                                is AudioOperationResult.Success -> {
                                    delay(1000)
                                    scanAudioFiles(false)
                                    showRenameDialog = false
                                }
                                is AudioOperationResult.NeedPermission -> {
                                    // Android 10+ 需要用户确认
                                    val request = IntentSenderRequest.Builder(result.intentSender).build()
                                    intentSenderLauncher.launch(request)
                                    showRenameDialog = false
                                }
                                is AudioOperationResult.Failed -> {
                                    showRenameDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog && selectedAudio != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除音频") },
            text = { Text("确定要删除 \"${selectedAudio!!.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val result = audioManager.deleteAudio(selectedAudio!!.uri)) {
                                is AudioOperationResult.Success -> {
                                    audioList = audioList.filter { it.id != selectedAudio!!.id }
                                    showDeleteDialog = false
                                }
                                is AudioOperationResult.NeedPermission -> {
                                    // Android 10+ 需要用户确认
                                    val request = IntentSenderRequest.Builder(result.intentSender).build()
                                    intentSenderLauncher.launch(request)
                                    showDeleteDialog = false
                                }
                                is AudioOperationResult.Failed -> {
                                    showDeleteDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 分类选择对话框（单选）
    if (showCategoryDialog && selectedAudio != null) {
        var selectedCategoryId by remember { 
            mutableStateOf<String?>(null)
        }
        
        LaunchedEffect(selectedAudio) {
            selectedCategoryId = categoryManager.getAudioCategory(selectedAudio!!.id)
        }
        
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("选择分类") },
            text = {
                LazyColumn {
                    // 无分类选项
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategoryId = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == null,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("无分类")
                        }
                    }
                    
                    items(categories.filter { it.id != AudioCategory.ALL.id }) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategoryId = category.id
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = category.id == selectedCategoryId,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name)
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                isAddCategoryFromSelection = true  // 从分类选择对话框触发
                                showAddCategoryDialog = true
                                showCategoryDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("新增分类")
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    // 删除分类按钮（左侧）
                    if (selectedCategoryId != null && categories.any { it.id == selectedCategoryId }) {
                        TextButton(
                            onClick = {
                                categoryToDelete = categories.find { it.id == selectedCategoryId }
                                showDeleteCategoryDialog = true
                                showCategoryDialog = false
                            }
                        ) {
                            Text("删除分类", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("取消")
                    }
                    
                    TextButton(
                        onClick = {
                            scope.launch {
                                categoryManager.setAudioCategory(
                                    selectedAudio!!.id,
                                    selectedCategoryId
                                )
                                
                                // 刷新分类列表和触发映射刷新
                                categories = categoryManager.getCategories()
                                categoryMappingVersion++
                                
                                showCategoryDialog = false
                            }
                        }
                    ) {
                        Text("确定")
                    }
                }
            },
            dismissButton = {}
        )
    }
    
    // 新增分类对话框
    if (showAddCategoryDialog) {
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { 
                showAddCategoryDialog = false
                newCategoryName = ""
                errorMessage = null
            },
            title = { Text("新增分类") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { 
                            newCategoryName = it
                            errorMessage = null
                        },
                        label = { Text("分类名称") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = if (errorMessage != null) {
                            { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            // 检查分类名称是否已存在
                            val existingCategory = categories.find { 
                                it.name.equals(newCategoryName.trim(), ignoreCase = true) 
                            }
                            
                            if (existingCategory != null) {
                                errorMessage = "分类名称已存在"
                            } else {
                                scope.launch {
                                    categoryManager.addCategory(newCategoryName.trim())
                                    categories = categoryManager.getCategories()
                                    newCategoryName = ""
                                    errorMessage = null
                                    showAddCategoryDialog = false
                                    
                                    // 如果是从分类选择对话框触发的，返回到分类选择对话框
                                    if (isAddCategoryFromSelection) {
                                        showCategoryDialog = true
                                    }
                                }
                            }
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddCategoryDialog = false
                    newCategoryName = ""
                    errorMessage = null
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除分类确认对话框
    if (showDeleteCategoryDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteCategoryDialog = false
                categoryToDelete = null
            },
            title = { Text("删除分类") },
            text = { Text("确定要删除分类 \"${categoryToDelete!!.name}\" 吗？该分类下的音频将变为未分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            categoryManager.deleteCategory(categoryToDelete!!.id)
                            categories = categoryManager.getCategories()
                            
                            // 如果当前选中的是被删除的分类，切换到"全部"
                            if (selectedCategory.id == categoryToDelete!!.id) {
                                selectedCategory = AudioCategory.ALL
                            }
                            
                            categoryToDelete = null
                            showDeleteCategoryDialog = false
                            // 问题2修复：删除分类后不再显示选择分类弹窗
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteCategoryDialog = false
                    categoryToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 分类管理对话框（长按分类标签时显示）
    if (showCategoryManageDialog && categoryToEdit != null) {
        val sheetState = rememberModalBottomSheetState()
        
        ModalBottomSheet(
            onDismissRequest = { 
                showCategoryManageDialog = false
                categoryToEdit = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 标题
                Text(
                    text = categoryToEdit!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 重命名选项
                Surface(
                    onClick = {
                        renameText = categoryToEdit!!.name
                        showRenameCategoryDialog = true
                        showCategoryManageDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "重命名",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // 删除选项
                Surface(
                    onClick = {
                        categoryToDelete = categoryToEdit
                        showDeleteCategoryDialog = true
                        showCategoryManageDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // 重命名分类对话框
    if (showRenameCategoryDialog && categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { 
                showRenameCategoryDialog = false
                categoryToEdit = null
            },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            scope.launch {
                                categoryManager.renameCategory(categoryToEdit!!.id, renameText)
                                categories = categoryManager.getCategories()
                                
                                renameText = ""
                                showRenameCategoryDialog = false
                                categoryToEdit = null
                            }
                        }
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameCategoryDialog = false
                    categoryToEdit = null
                    renameText = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 跳过时间设置对话框
    if (showSeekTimeDialog) {
        SeekTimeDialog(
            currentSeconds = seekTimeInSeconds,
            onConfirm = { seconds ->
                seekTimeInSeconds = seconds
                prefs.edit().putInt("seek_time", seconds).apply()
                showSeekTimeDialog = false
            },
            onDismiss = { showSeekTimeDialog = false }
        )
    }
    
    // 自动回退设置对话框
    if (showAutoRewindDialog) {
        AutoRewindDialog(
            currentSeconds = autoRewindInSeconds,
            onConfirm = { seconds ->
                autoRewindInSeconds = seconds
                prefs.edit().putInt("auto_rewind", seconds).apply()
                showAutoRewindDialog = false
            },
            onDismiss = { showAutoRewindDialog = false }
        )
    }
}

/**
 * 音频列表项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioItem(
    audio: LocalAudioFile,
    isReorderMode: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val positionManager = remember { com.xmvisio.app.audio.PlaybackPositionManager(context) }
    val globalPlayer = remember { com.xmvisio.app.audio.GlobalAudioPlayer.getInstance(context) }
    
    // 检查是否正在播放这个音频
    val isPlaying by globalPlayer.isPlaying.collectAsState()
    val currentPlayingAudioId by globalPlayer.currentAudioId.collectAsState()
    val isThisAudioPlaying = isPlaying && currentPlayingAudioId == audio.id
    
    // 获取播放位置和进度 - 实时更新
    val currentPosition by globalPlayer.currentPosition.collectAsState()
    val savedPosition = if (isThisAudioPlaying) {
        currentPosition
    } else {
        val saved by produceState(
            initialValue = kotlin.time.Duration.ZERO,
            key1 = audio.id
        ) {
            value = positionManager.getPosition(audio.id)
        }
        saved
    }
    
    val duration = remember(audio.duration) { audio.duration.milliseconds }
    
    val progress = remember(savedPosition, duration) {
        if (duration.inWholeMilliseconds > 0) {
            (savedPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    val remainingTime = remember(savedPosition, duration) {
        val remaining = duration - savedPosition
        val totalSeconds = remaining.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick,
                enabled = !isReorderMode
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = if (isDragging) 4.dp else 0.dp
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面（带播放动画）
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
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // 播放动画遮罩
                    if (isThisAudioPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            com.xmvisio.app.ui.foundation.PlayingAnimation(
                                modifier = Modifier.size(28.dp, 20.dp),
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 信息
                Column(modifier = Modifier.weight(1f)) {
                    // 标题（只有正在播放时才有跑马灯效果）
                    Text(
                        text = audio.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isThisAudioPlaying) Modifier.basicMarquee() else Modifier
                    )
                    
                    // 艺术家（只有正在播放时才有跑马灯效果）
                    if (audio.artist != null) {
                        Text(
                            text = audio.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (isThisAudioPlaying) Modifier.basicMarquee() else Modifier
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 剩余时间和进度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (progress > 0f) remainingTime else formatTime(audio.duration / 1000),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (progress > 0f) {
                            Text(
                                text = "已播${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 排序模式下显示拖拽手柄
                if (isReorderMode) {
                    Box(
                        modifier = dragHandleModifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "拖动排序",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 跳过时间设置对话框
 */
@Composable
private fun SeekTimeDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeconds by remember { mutableStateOf(currentSeconds) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳过时间") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "设置快进/快退的时间间隔",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 滑块
                Column {
                    Slider(
                        value = selectedSeconds.toFloat(),
                        onValueChange = { selectedSeconds = it.toInt() },
                        valueRange = 3f..60f,
                        steps = 56,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${selectedSeconds}秒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "60秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSeconds) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 自动回退设置对话框
 */
@Composable
private fun AutoRewindDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeconds by remember { mutableStateOf(currentSeconds) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动回退") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "暂停后恢复播放时自动回退的秒数",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 滑块
                Column {
                    Slider(
                        value = selectedSeconds.toFloat(),
                        onValueChange = { selectedSeconds = it.toInt() },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${selectedSeconds}秒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "20秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSeconds) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
