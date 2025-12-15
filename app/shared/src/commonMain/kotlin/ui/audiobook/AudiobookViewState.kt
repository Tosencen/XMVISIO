package com.xmvisio.app.ui.audiobook

import com.xmvisio.app.data.audiobook.AudiobookId
import com.xmvisio.app.data.audiobook.BookCategory

/**
 * 有声书列表项视图状态
 */
data class AudiobookItemViewState(
    val id: AudiobookId,
    val name: String,
    val author: String?,
    val coverPath: String?,
    val progress: Float,
    val remainingTime: String,
    val category: BookCategory,
    val isPlaying: Boolean = false
)

/**
 * 有声书页面视图状态
 */
data class AudiobookViewState(
    val books: Map<BookCategory, List<AudiobookItemViewState>> = emptyMap(),
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val searchQuery: String = "",
    val searchActive: Boolean = false,
    val playButtonState: PlayButtonState? = null,
    val isLoading: Boolean = false,
    val showAddBookHint: Boolean = false
)

/**
 * 布局模式
 */
enum class LayoutMode {
    LIST,   // 列表模式
    GRID    // 网格模式
}

/**
 * 播放按钮状态
 */
enum class PlayButtonState {
    PLAYING,    // 播放中
    PAUSED      // 暂停
}
