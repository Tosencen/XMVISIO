package com.xmvisio.app.ui.audiobook

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.audiobook.AudiobookId
import com.xmvisio.app.data.audiobook.BookCategory
import com.xmvisio.app.ui.foundation.PlayingAnimation

/**
 * 列表模式显示有声书
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListBooks(
    books: Map<BookCategory, List<AudiobookItemViewState>>,
    onBookClick: (AudiobookId) -> Unit,
    onBookLongClick: (AudiobookId) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            top = 24.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        )
    ) {
        books.forEach { (category, bookList) ->
            if (bookList.isEmpty()) return@forEach
            
            // 分类标题
            stickyHeader(
                key = category,
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
            
            // 书籍列表
            items(
                items = bookList,
                key = { it.id.value },
                contentType = { "book" }
            ) { book ->
                ListBookRow(
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
 * 列表书籍行
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBookRow(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面（带播放动画）
            Box(
                modifier = Modifier.size(80.dp)
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
                            modifier = Modifier.size(32.dp, 24.dp),
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 书籍信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 作者（跑马灯效果）
                if (book.author != null) {
                    Text(
                        text = book.author.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
                
                // 书名（跑马灯效果）
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 进度条
                if (book.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 剩余时间和进度百分比
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.remainingTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (book.progress > 0f) {
                        Text(
                            text = "${(book.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}
