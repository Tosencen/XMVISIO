package com.xmvisio.app.ui.folder

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.MediaHolder
import com.xmvisio.app.data.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaView(
    mediaHolder: MediaHolder,
    isGridLayout: Boolean,
    isFolderTreeMode: Boolean = false,
    onFolderClick: (Folder) -> Unit,
    onFolderLongClick: ((Folder) -> Unit)? = null,
    onVideoClick: (VideoInfo) -> Unit,
    onVideoLongClick: ((VideoInfo) -> Unit)? = null,
    onAudioClick: (LocalAudioFile) -> Unit,
    onAudioLongClick: ((LocalAudioFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (isGridLayout) {
        MediaGridView(
            mediaHolder = mediaHolder,
            isFolderTreeMode = isFolderTreeMode,
            onFolderClick = onFolderClick,
            onFolderLongClick = onFolderLongClick,
            onVideoClick = onVideoClick,
            onVideoLongClick = onVideoLongClick,
            onAudioClick = onAudioClick,
            onAudioLongClick = onAudioLongClick,
            modifier = modifier
        )
    } else {
        MediaListView(
            mediaHolder = mediaHolder,
            isFolderTreeMode = isFolderTreeMode,
            onFolderClick = onFolderClick,
            onFolderLongClick = onFolderLongClick,
            onVideoClick = onVideoClick,
            onVideoLongClick = onVideoLongClick,
            onAudioClick = onAudioClick,
            onAudioLongClick = onAudioLongClick,
            modifier = modifier
        )
    }
}

@Composable
private fun MediaGridView(
    mediaHolder: MediaHolder,
    isFolderTreeMode: Boolean,
    onFolderClick: (Folder) -> Unit,
    onFolderLongClick: ((Folder) -> Unit)? = null,
    onVideoClick: (VideoInfo) -> Unit,
    onVideoLongClick: ((VideoInfo) -> Unit)? = null,
    onAudioClick: (LocalAudioFile) -> Unit,
    onAudioLongClick: ((LocalAudioFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val folderMinWidth = 90.dp
    val videoMinWidth = 130.dp

    BoxWithConstraints(modifier = modifier) {
        val contentPadding = 8.dp
        val itemSpacing = 2.dp
        val maxWidth = this.maxWidth - contentPadding * 2 - itemSpacing
        val maxFolders = (maxWidth / folderMinWidth).toInt().coerceAtLeast(1)
        val maxVideos = (maxWidth / videoMinWidth).toInt().coerceAtLeast(1)
        val spans = lcm(maxFolders, maxVideos)
        val singleFolderSpan = spans / maxFolders
        val singleVideoSpan = spans / maxVideos

        LazyVerticalGrid(
            columns = GridCells.Fixed(spans),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = contentPadding, vertical = contentPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            if (mediaHolder.folders.isNotEmpty()) {
                if (isFolderTreeMode) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "文件夹 (${mediaHolder.folders.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                itemsIndexed(
                    items = mediaHolder.folders,
                    span = { _, _ -> GridItemSpan(singleFolderSpan) }
                ) { _, folder ->
                    FolderGridCard(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onLongClick = { onFolderLongClick?.invoke(folder) }
                    )
                }
            }

            if (mediaHolder.videos.isNotEmpty()) {
                if (isFolderTreeMode && mediaHolder.folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (isFolderTreeMode) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "视频 (${mediaHolder.videos.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                itemsIndexed(
                    items = mediaHolder.videos,
                    span = { _, _ -> GridItemSpan(singleVideoSpan) }
                ) { _, video ->
                    VideoGridCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick?.invoke(video) }
                    )
                }
            }

            if (mediaHolder.audios.isNotEmpty()) {
                if (isFolderTreeMode) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "音频 (${mediaHolder.audios.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                itemsIndexed(
                    items = mediaHolder.audios,
                    span = { _, _ -> GridItemSpan(singleVideoSpan) }
                ) { _, audio ->
                    AudioGridCard(
                        audio = audio,
                        onClick = { onAudioClick(audio) },
                        onLongClick = { onAudioLongClick?.invoke(audio) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaListView(
    mediaHolder: MediaHolder,
    isFolderTreeMode: Boolean,
    onFolderClick: (Folder) -> Unit,
    onFolderLongClick: ((Folder) -> Unit)? = null,
    onVideoClick: (VideoInfo) -> Unit,
    onVideoLongClick: ((VideoInfo) -> Unit)? = null,
    onAudioClick: (LocalAudioFile) -> Unit,
    onAudioLongClick: ((LocalAudioFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (mediaHolder.folders.isNotEmpty()) {
            if (isFolderTreeMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "文件夹 (${mediaHolder.folders.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            itemsIndexed(mediaHolder.folders) { _, folder ->
                FolderListCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick?.invoke(folder) }
                )
            }
        }

        if (mediaHolder.videos.isNotEmpty()) {
            if (isFolderTreeMode && mediaHolder.folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (isFolderTreeMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "视频 (${mediaHolder.videos.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            itemsIndexed(mediaHolder.videos) { _, video ->
                VideoListCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onLongClick = { onVideoLongClick?.invoke(video) }
                )
            }
        }

        if (mediaHolder.audios.isNotEmpty()) {
            if (isFolderTreeMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "音频 (${mediaHolder.audios.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            itemsIndexed(mediaHolder.audios) { _, audio ->
                AudioListCard(
                    audio = audio,
                    onClick = { onAudioClick(audio) },
                    onLongClick = { onAudioLongClick?.invoke(audio) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderGridCard(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                painter = folderPainter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .width(min(90.dp, LocalConfiguration.current.screenWidthDp.dp * 0.3f))
                    .aspectRatio(20 / 17f),
            )
            // 总时长角标：仅在总时长为 0（未知/仅含子文件夹）时隐藏，避免误显示 00:00
            if (folder.totalDuration > 0) {
                Text(
                    text = formatDuration(folder.totalDuration),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = Color.White,
                    modifier = Modifier
                        .padding(5.dp)
                        .padding(bottom = 3.dp)
                        .align(Alignment.BottomEnd)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 1.dp, horizontal = 3.dp),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = folder.name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val stats = buildString {
                if (folder.mediaCount > 0) append("${folder.mediaCount} 视频")
                if (folder.foldersCount > 0) {
                    if (isNotEmpty()) append(", ")
                    append("${folder.foldersCount} 文件夹")
                }
            }
            if (stats.isNotEmpty()) {
                Text(
                    text = stats,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderListCard(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 文件夹图标 + 总时长角标（与网格布局一致）
            Box {
                Icon(
                    painter = folderPainter(),
                    contentDescription = null,
                    modifier = Modifier
                        .width(min(90.dp, LocalConfiguration.current.screenWidthDp.dp * 0.3f))
                        .aspectRatio(20f / 17f),
                    tint = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                if (folder.totalDuration > 0) {
                    Text(
                        text = formatDuration(folder.totalDuration),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = Color.White,
                        modifier = Modifier
                            .padding(5.dp)
                            .padding(bottom = 3.dp)
                            .align(Alignment.BottomEnd)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 1.dp, horizontal = 3.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val stats = buildString {
                    if (folder.mediaCount > 0) append("${folder.mediaCount} 个文件")
                    if (folder.foldersCount > 0) {
                        if (isNotEmpty()) append(", ")
                        append("${folder.foldersCount} 个子文件夹")
                    }
                }
                if (stats.isNotEmpty()) {
                    Text(
                        text = stats,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridCard(
    video: VideoInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val uri = android.net.Uri.parse(video.uri)
                    thumbnail = context.contentResolver.loadThumbnail(
                        uri, android.util.Size(512, 384), null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    thumbnail = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver, video.id,
                        android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                    )
                }
            } catch (e: Exception) {
                Log.w("MediaView", "加载视频缩略图失败", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        painter = BitmapPainter(thumbnail!!.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.4f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (video.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(5.dp)
                            .align(Alignment.BottomEnd),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal)
                        )
                    }
                }
            }
            Text(
                text = video.name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListCard(
    video: VideoInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val uri = android.net.Uri.parse(video.uri)
                    thumbnail = context.contentResolver.loadThumbnail(
                        uri, android.util.Size(512, 384), null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    thumbnail = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver, video.id,
                        android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                    )
                }
            } catch (e: Exception) {
                Log.w("MediaView", "加载视频缩略图失败", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        painter = BitmapPainter(thumbnail!!.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.4f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (video.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(3.dp)
                            .align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioGridCard(
    audio: LocalAudioFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioListCard(
    audio: LocalAudioFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                if (audio.artist != null) {
                    Text(
                        text = audio.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun lcm(a: Int, b: Int): Int {
    return kotlin.math.abs(a * b) / gcd(a, b)
}

private fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}

@Composable
private fun folderPainter(): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier("folder_thumb", "drawable", context.packageName)
    return painterResource(resId)
}
