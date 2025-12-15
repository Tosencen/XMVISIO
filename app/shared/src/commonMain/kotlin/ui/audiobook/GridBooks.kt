package com.xmvisio.app.ui.audiobook

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.audiobook.AudiobookId
import com.xmvisio.app.data.audiobook.BookCategory
import com.xmvisio.app.ui.foundation.PlayingAnimation

/**
 * 网格模式显示有声书
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridBooks(
    books: Map<BookCategory, List<AudiobookItemViewState>>,
    onBookClick: (AudiobookId) -> Unit,
    onBookLongClick: (AudiobookId) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            top = 24.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        )
    ) {
        books.forEach { (category, bookList) ->
            if (bookList.isEmpty()) return@forEach
            
            // 分类标题（占满一行）
            item(
                key = category,
                span = { GridItemSpan(2) },
                contentType = "header"
            ) {
                CategoryHeader(
                    category = category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(vertical = 8.dp)
                )
            }
            
            // 书籍网格
            items(
                items = bookList,
                key = { it.id.value },
                contentType = { "book" }
            ) { book ->
                GridBookCard(
                    book = book,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick
                )
            }
        }
    }
}

/**
 * 分类标题
 */
@Composable
private fun CategoryHeader(
    category: BookCategory,
    modifier: Modifier = Modifier
) {
    val title = when (category) {
        BookCategory.CURRENT -> "当前阅读"
        BookCategory.FINISHED -> "已完成"
    }
    
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * 网格书籍卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBookCard(
    book: AudiobookItemViewState,
    onBookClick: (AudiobookId) -> Unit,
    onBookLongClick: (AudiobookId) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onBookClick(book.id) },
                onLongClick = { onBookLongClick(book.id) }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 封面（带播放动画）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                BookCover(
                    coverPath = book.coverPath,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 播放动画遮罩
                if (book.isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingAnimation(
                            modifier = Modifier.size(40.dp, 32.dp),
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 书名（跑马灯效果）
            Text(
                text = book.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .height(40.dp)
                    .basicMarquee()
            )
            
            // 作者（跑马灯效果）
            if (book.author != null) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 进度百分比
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.remainingTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (book.progress > 0f) {
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 书籍封面
 */
@Composable
private fun BookCover(
    coverPath: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // TODO: 使用 Coil 加载封面图片
            // 暂时显示占位符
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}
