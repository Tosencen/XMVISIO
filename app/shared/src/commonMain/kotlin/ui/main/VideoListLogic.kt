package com.xmvisio.app.ui.main

import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.VideoInfo

/**
 * 视频列表的纯逻辑（无 Android 依赖，可单元测试）
 *
 * 从 [VideoScreenState] 中提取，保证 UI 状态与计算逻辑分离。
 * 注意：路径计算全部用字符串操作，避免依赖 java.io.File（commonMain 不可用）。
 */

/**
 * 返回路径的父目录（与 java.io.File.parent 语义一致）：
 * - 无 '/' 时返回 null
 * - 根目录文件（如 "/a.mp4"）父目录为 "/"
 */
internal fun pathParent(path: String): String? {
    val idx = path.lastIndexOf('/')
    return when {
        idx < 0 -> null
        idx == 0 -> "/"
        else -> path.substring(0, idx)
    }
}

/** 返回路径的文件/目录名 */
internal fun pathName(path: String): String {
    val idx = path.lastIndexOf('/')
    return if (idx < 0) path else path.substring(idx + 1)
}

/**
 * 按日期、大小、时长、名称排序（sortBy: 0=名称, 1=日期, 2=大小, 3=时长；其余默认日期）
 * @param sortAscending true 升序，false 降序
 */
internal fun computeSortedVideos(
    videos: List<VideoInfo>,
    sortBy: Int,
    sortAscending: Boolean
): List<VideoInfo> {
    val comparator = when (sortBy) {
        0 -> compareBy<VideoInfo> { it.name.lowercase() }
        1 -> compareBy<VideoInfo> { it.dateModified }
        2 -> compareBy<VideoInfo> { it.size }
        3 -> compareBy<VideoInfo> { it.duration }
        else -> compareBy<VideoInfo> { it.dateModified }
    }
    return videos.sortedWith(if (sortAscending) comparator else comparator.reversed())
}

/**
 * 将视频按所在目录聚合成文件夹统计列表
 */
internal fun computeFolderHierarchy(videos: List<VideoInfo>): List<Folder> {
    val grouped = videos
        .filter { it.path.isNotEmpty() }
        .groupBy { pathParent(it.path) ?: "" }
    return grouped.map { (path, vids) ->
        Folder(
            name = pathName(path),
            path = path,
            mediaCount = vids.size,
            totalDuration = vids.sumOf { it.duration },
            totalSize = vids.sumOf { it.size },
            dateModified = vids.maxOfOrNull { it.dateModified } ?: 0L,
            parentPath = pathParent(path),
            foldersCount = 0
        )
    }.filter { it.path.isNotEmpty() }
}

/**
 * 计算当前层级的文件夹树：
 * - 未进入任何文件夹：只显示顶层文件夹（其父目录不在已有文件夹集合中）
 * - 已进入文件夹：只显示该文件夹的直接子文件夹
 */
internal fun computeFolderTree(
    folders: List<Folder>,
    currentFolderPath: String?
): List<Folder> {
    val pathSet = folders.map { it.path }.toSet()
    return if (currentFolderPath == null) {
        folders.filter { it.parentPath !in pathSet }
    } else {
        folders.filter { it.parentPath == currentFolderPath }
    }
}

/**
 * 计算当前层级显示的视频列表（viewMode: 0=树形, 1=文件夹, 2=全部）
 */
internal fun computeFolderVideos(
    sortedVideos: List<VideoInfo>,
    viewMode: Int,
    currentFolderPath: String?,
    folderPathSet: Set<String>
): List<VideoInfo> {
    if (viewMode == 2) return sortedVideos
    return if (currentFolderPath != null) {
        sortedVideos.filter { pathParent(it.path) == currentFolderPath }
    } else {
        sortedVideos.filter { it.path.isEmpty() || pathParent(it.path) !in folderPathSet }
    }
}

/**
 * 计算面包屑导航路径（从根到当前文件夹）
 */
internal fun computeBreadcrumbPath(
    folders: List<Folder>,
    currentFolderPath: String?
): List<String> {
    if (currentFolderPath == null) return emptyList()
    val pathList = mutableListOf<String>()
    var current: String? = currentFolderPath
    while (current != null) {
        pathList.add(0, current)
        val folder = folders.find { it.path == current }
        current = folder?.parentPath?.let { parent ->
            // 存储根目录不再继续向上（与 Android 路径约定一致）
            if (parent == "/storage/emulated/0" || parent == "/sdcard") null else parent
        }
    }
    return pathList
}
