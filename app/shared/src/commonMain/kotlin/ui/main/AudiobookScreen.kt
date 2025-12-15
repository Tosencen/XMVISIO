package com.xmvisio.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.audiobook.AudiobookId
import com.xmvisio.app.data.audiobook.BookCategory
import com.xmvisio.app.ui.audiobook.*

/**
 * 有声书页面
 * 平台特定实现
 */
@Composable
expect fun AudiobookScreen(
    onNavigateToPlayer: (Any) -> Unit = {},
    modifier: Modifier = Modifier
)

/**
 * 有声书页面（通用实现）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookScreenCommon(
    modifier: Modifier = Modifier
) {
    // 临时状态管理（后续会用 ViewModel）
    var layoutMode by remember { mutableStateOf(LayoutMode.LIST) }
    var showEmptyState by remember { mutableStateOf(true) }  // 控制是否显示空状态
    val viewState = remember { createMockViewState() }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("有声书") },
                actions = {
                    // 临时：切换空状态/列表状态（用于测试）
                    TextButton(
                        onClick = { showEmptyState = !showEmptyState }
                    ) {
                        Text(if (showEmptyState) "显示列表" else "显示空状态")
                    }
                    
                    // 布局切换按钮
                    if (!showEmptyState) {
                        IconButton(
                            onClick = {
                                layoutMode = if (layoutMode == LayoutMode.LIST) {
                                    LayoutMode.GRID
                                } else {
                                    LayoutMode.LIST
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (layoutMode == LayoutMode.LIST) {
                                    Icons.Default.GridView
                                } else {
                                    Icons.Default.ViewList
                                },
                                contentDescription = "切换布局"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        floatingActionButton = {
            // 播放按钮（如果有正在播放的书）
            if (viewState.playButtonState != null) {
                FloatingActionButton(
                    onClick = { /* TODO: 播放/暂停 */ },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = if (viewState.playButtonState == PlayButtonState.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "播放/暂停"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 显示空状态或书籍列表
            if (showEmptyState) {
                EmptyState(
                    title = "还没有有声书",
                    subtitle = "点击右上角添加按钮导入本地音频文件\n或从网络下载有声书资源"
                )
            } else {
                when (layoutMode) {
                    LayoutMode.LIST -> {
                        ListBooks(
                            books = viewState.books,
                            onBookClick = { /* TODO: 打开播放器 */ },
                            onBookLongClick = { /* TODO: 显示菜单 */ }
                        )
                    }
                    LayoutMode.GRID -> {
                        GridBooks(
                            books = viewState.books,
                            onBookClick = { /* TODO: 打开播放器 */ },
                            onBookLongClick = { /* TODO: 显示菜单 */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 创建模拟数据（用于预览）
 */
private fun createMockViewState(): AudiobookViewState {
    val mockBooks = listOf(
        AudiobookItemViewState(
            id = AudiobookId("1"),
            name = "三体",
            author = "刘慈欣",
            coverPath = null,
            progress = 0.35f,
            remainingTime = "04:32:15",
            category = BookCategory.CURRENT
        ),
        AudiobookItemViewState(
            id = AudiobookId("2"),
            name = "活着",
            author = "余华",
            coverPath = null,
            progress = 0.68f,
            remainingTime = "01:45:30",
            category = BookCategory.CURRENT
        ),
        AudiobookItemViewState(
            id = AudiobookId("3"),
            name = "百年孤独",
            author = "加西亚·马尔克斯",
            coverPath = null,
            progress = 1.0f,
            remainingTime = "00:00:00",
            category = BookCategory.FINISHED
        ),
        AudiobookItemViewState(
            id = AudiobookId("4"),
            name = "人类简史",
            author = "尤瓦尔·赫拉利",
            coverPath = null,
            progress = 0.15f,
            remainingTime = "12:30:00",
            category = BookCategory.CURRENT
        ),
        AudiobookItemViewState(
            id = AudiobookId("5"),
            name = "1984",
            author = "乔治·奥威尔",
            coverPath = null,
            progress = 1.0f,
            remainingTime = "00:00:00",
            category = BookCategory.FINISHED
        )
    )
    
    val groupedBooks = mockBooks.groupBy { it.category }
    
    return AudiobookViewState(
        books = groupedBooks,
        layoutMode = LayoutMode.LIST,
        playButtonState = PlayButtonState.PAUSED
    )
}
