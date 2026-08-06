package com.xmvisio.app.data

import com.xmvisio.app.audio.LocalAudioFile

/**
 * 媒体持有者 - 用于在文件夹树中传递数据
 */
data class MediaHolder(
    val folders: List<Folder> = emptyList(),
    val videos: List<VideoInfo> = emptyList(),
    val audios: List<LocalAudioFile> = emptyList(),
)
