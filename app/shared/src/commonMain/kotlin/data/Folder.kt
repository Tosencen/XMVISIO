package com.xmvisio.app.data

/**
 * 文件夹 UI 展示模型
 * 包含文件夹的统计信息，用于界面展示
 */
data class Folder(
    val name: String,
    val path: String,
    val dateModified: Long = 0,
    val parentPath: String? = null,
    val totalSize: Long = 0,
    val totalDuration: Long = 0,
    val mediaCount: Int = 0,
    val foldersCount: Int = 0,
) {
    companion object {
        val sample = Folder(
            name = "示例文件夹",
            path = "/storage/emulated/0/Movies",
            dateModified = System.currentTimeMillis(),
        )
    }
}

/**
 * 查找最接近的父文件夹
 */
fun List<Folder>.findClosestFolder(mediaPath: String): Folder? {
    val mediaDirectory = mediaPath.substringBeforeLast("/")
    return filter { folder ->
        mediaDirectory == folder.path || mediaDirectory.startsWith(folder.path + "/")
    }.maxByOrNull { it.path.length }
}
