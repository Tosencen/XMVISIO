package com.xmvisio.app.folder

import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.FolderType
import com.xmvisio.app.data.MediaHolder
import com.xmvisio.app.data.SortBy
import com.xmvisio.app.data.SortOrder
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.audio.LocalAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * 树形目录 UseCase
 * 负责构建文件夹树形结构和媒体列表
 */
class GetFolderTreeUseCase(
    private val folderRepository: FolderRepository,
    private val folderScanner: FolderScanner
) {

    /**
     * 获取指定路径下的媒体持有者
     *
     * @param folderPath 文件夹路径，null 表示根级
     * @param type 媒体类型过滤
     * @param sortBy 排序方式
     * @param sortOrder 排序方向
     */
    suspend fun invoke(
        folderPath: String? = null,
        type: FolderType? = null,
        sortBy: SortBy = SortBy.DATE,
        sortOrder: SortOrder = SortOrder.DESCENDING
    ): Flow<MediaHolder> = flow {
        if (folderPath == null) {
            // 根级：显示用户添加的所有文件夹
            emit(getRootFolders(type, sortBy, sortOrder))
        } else {
            // 子级：显示指定路径下的内容
            emit(getMediaUnder(folderPath, type, sortBy, sortOrder))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取根级文件夹列表
     */
    private suspend fun getRootFolders(
        type: FolderType?,
        sortBy: SortBy,
        sortOrder: SortOrder
    ): MediaHolder {
        val folders = folderRepository.getRootFolders(type)
        val mediaFolders = folders.map { mediaFolder ->
            buildFolderFromMediaFolder(mediaFolder)
        }.let { sortFolders(it, sortBy, sortOrder) }

        return MediaHolder(folders = mediaFolders)
    }

    /**
     * 获取指定路径下的媒体内容
     */
    private suspend fun getMediaUnder(
        path: String,
        type: FolderType?,
        sortBy: SortBy,
        sortOrder: SortOrder
    ): MediaHolder {
        // 获取直接子文件夹
        val subFolderPaths = folderScanner.getSubFolders(path)
        val subFolders = subFolderPaths.mapNotNull { subPath ->
            // 检查是否是用户添加的文件夹
            val mediaFolder = folderRepository.getFolderByPath(subPath)
            buildFolderFromPath(subPath, mediaFolder?.name)
        }.let { sortFolders(it, sortBy, sortOrder) }

        // 获取当前文件夹下的媒体文件
        val videos = mutableListOf<VideoInfo>()
        val audios = mutableListOf<LocalAudioFile>()

        if (type == null || type == FolderType.VIDEO || type == FolderType.ALL) {
            videos.addAll(folderScanner.scanVideosInFolder(path))
        }

        if (type == null || type == FolderType.MUSIC || type == FolderType.ALL) {
            audios.addAll(folderScanner.scanAudiosInFolder(path))
        }

        return MediaHolder(
            folders = subFolders,
            videos = sortVideos(videos, sortBy, sortOrder),
            audios = sortAudios(audios, sortBy, sortOrder)
        )
    }

    /**
     * 从 MediaFolder 构建 Folder
     */
    private suspend fun buildFolderFromMediaFolder(mediaFolder: com.xmvisio.app.data.MediaFolder): Folder {
        val videos = folderScanner.scanVideosInFolder(mediaFolder.path)
        val audios = folderScanner.scanAudiosInFolder(mediaFolder.path)
        val subFolders = folderScanner.getSubFolders(mediaFolder.path)

        return Folder(
            name = mediaFolder.name,
            path = mediaFolder.path,
            dateModified = folderScanner.getFolderLastModified(mediaFolder.path),
            parentPath = mediaFolder.parentPath,
            totalSize = videos.sumOf { it.size } + audios.sumOf { it.duration },
            totalDuration = videos.sumOf { it.duration } + audios.sumOf { it.duration },
            mediaCount = videos.size + audios.size,
            foldersCount = subFolders.size
        )
    }

    /**
     * 从路径构建 Folder
     */
    private suspend fun buildFolderFromPath(path: String, name: String? = null): Folder {
        val dir = File(path)
        val videos = folderScanner.scanVideosInFolder(path)
        val audios = folderScanner.scanAudiosInFolder(path)
        val subFolders = folderScanner.getSubFolders(path)

        return Folder(
            name = name ?: dir.name,
            path = path,
            dateModified = dir.lastModified(),
            parentPath = dir.parent,
            totalSize = videos.sumOf { it.size } + audios.sumOf { it.duration },
            totalDuration = videos.sumOf { it.duration } + audios.sumOf { it.duration },
            mediaCount = videos.size + audios.size,
            foldersCount = subFolders.size
        )
    }

    /**
     * 排序文件夹列表
     */
    private fun sortFolders(folders: List<Folder>, sortBy: SortBy, sortOrder: SortOrder): List<Folder> {
        val sorted = when (sortBy) {
            SortBy.NAME -> folders.sortedBy { it.name.lowercase() }
            SortBy.DATE -> folders.sortedBy { it.dateModified }
            SortBy.SIZE -> folders.sortedBy { it.totalSize }
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    /**
     * 排序视频列表
     */
    private fun sortVideos(videos: List<VideoInfo>, sortBy: SortBy, sortOrder: SortOrder): List<VideoInfo> {
        val sorted = when (sortBy) {
            SortBy.NAME -> videos.sortedBy { it.name.lowercase() }
            SortBy.DATE -> videos.sortedBy { it.dateModified }
            SortBy.SIZE -> videos.sortedBy { it.size }
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    /**
     * 排序音频列表
     */
    private fun sortAudios(audios: List<LocalAudioFile>, sortBy: SortBy, sortOrder: SortOrder): List<LocalAudioFile> {
        val sorted = when (sortBy) {
            SortBy.NAME -> audios.sortedBy { it.title.lowercase() }
            SortBy.DATE -> audios.sortedBy { it.dateAdded }
            SortBy.SIZE -> audios // 音频没有 size 字段，保持原顺序
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    companion object {
        @Volatile
        private var instance: GetFolderTreeUseCase? = null

        fun getInstance(
            folderRepository: FolderRepository,
            folderScanner: FolderScanner
        ): GetFolderTreeUseCase {
            return instance ?: synchronized(this) {
                instance ?: GetFolderTreeUseCase(folderRepository, folderScanner).also {
                    instance = it
                }
            }
        }
    }
}
