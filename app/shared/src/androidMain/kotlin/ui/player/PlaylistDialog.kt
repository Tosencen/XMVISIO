package com.xmvisio.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.ui.foundation.PlayingAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDialog(
    playlist: List<LocalAudioFile>,
    currentAudioId: Long,
    isPlaying: Boolean,
    onAudioClick: (LocalAudioFile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(currentAudioId) {
        val currentIndex = playlist.indexOfFirst { it.id == currentAudioId }
        if (currentIndex >= 0) {
            val scrollToIndex = (currentIndex - 1).coerceAtLeast(0)
            listState.scrollToItem(scrollToIndex)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Text(
                text = "播放列表 (${playlist.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = playlist,
                    key = { it.id }
                ) { audio ->
                    val isCurrentAudio = audio.id == currentAudioId
                    val backgroundColor = if (isCurrentAudio) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    }
                    
                    ListItem(
                        headlineContent = {
                            Text(
                                text = audio.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrentAudio) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        },
                        supportingContent = if (audio.artist != null) {
                            {
                                Text(
                                    text = audio.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrentAudio) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        } else null,
                        leadingContent = if (isCurrentAudio && isPlaying) {
                            {
                                PlayingAnimation(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else if (isCurrentAudio) {
                            {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = backgroundColor
                        ),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clickable {
                                onAudioClick(audio)
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}
