package com.xmvisio.app.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xmvisio.app.audio.AudioScanner
import com.xmvisio.app.audio.GlobalAudioPlayer
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.ui.foundation.PlayingAnimation
import kotlinx.coroutines.launch

/**
 * 播放列表对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDialog(
    currentAudio: LocalAudioFile,
    onAudioSelect: (LocalAudioFile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioScanner = remember { AudioScanner(context) }
    val globalPlayer = remember { GlobalAudioPlayer.getInstance(context) }
    
    var audioList by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 检查当前播放状态
    val isPlaying by globalPlayer.isPlaying.collectAsState()
    val currentPlayingAudioId = globalPlayer.getCurrentAudioId()
    
    // 加载音频列表
    LaunchedEffect(Unit) {
        scope.launch {
            audioList = audioScanner.scanAudioFiles()
            isLoading = false
        }
    }
    
    // 滚动到当前播放的音频
    val listState = rememberLazyListState()
    LaunchedEffect(audioList, currentAudio.id) {
        if (audioList.isNotEmpty()) {
            val currentIndex = audioList.indexOfFirst { it.id == currentAudio.id }
            if (currentIndex >= 0) {
                listState.animateScrollToItem(currentIndex)
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            // 标题
            Text(
                text = "播放列表",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = audioList,
                        key = { it.id }
                    ) { audio ->
                        val isThisAudioPlaying = isPlaying && currentPlayingAudioId == audio.id
                        val isCurrentAudio = audio.id == currentAudio.id
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAudioSelect(audio)
                                    onDismiss()
                                }
                                .background(
                                    if (isCurrentAudio) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧播放动画或图标
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isThisAudioPlaying) {
                                    PlayingAnimation(
                                        modifier = Modifier.size(24.dp, 18.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AudioFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // 音频信息
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = audio.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrentAudio) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = if (isThisAudioPlaying) Modifier.basicMarquee() else Modifier
                                )
                                
                                if (audio.artist != null) {
                                    Text(
                                        text = audio.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
