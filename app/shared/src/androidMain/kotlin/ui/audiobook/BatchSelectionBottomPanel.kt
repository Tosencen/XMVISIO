package com.xmvisio.app.ui.audiobook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.AudioCategory

/**
 * 批量选择底部操作面板
 * 
 * @param visible 是否显示面板
 * @param selectedCount 选中的音频数量
 * @param categories 可用的分类列表（不包括"全部"）
 * @param onCategoryClick 点击分类时的回调
 * @param onDeleteClick 点击删除按钮的回调
 * @param onClose 点击关闭按钮的回调
 * @param modifier 修饰符
 */
@Composable
fun BatchSelectionBottomPanel(
    visible: Boolean,
    selectedCount: Int,
    categories: List<AudioCategory>,
    onCategoryClick: (AudioCategory) -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部：选中计数、删除按钮和关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选择 $selectedCount 项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除"
                            )
                        }
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 分类列表
                if (categories.isEmpty()) {
                    // 空状态提示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无分类，请先创建分类",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "添加到分类",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                category = category,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分类芯片
 */
@Composable
private fun CategoryChip(
    category: AudioCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
