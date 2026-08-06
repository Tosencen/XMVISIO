package com.xmvisio.app.data

import kotlinx.serialization.Serializable

/**
 * 媒体文件夹实体
 * 存储用户创建/添加的文件夹信息
 */
@Serializable
data class MediaFolder(
    val id: String,
    val name: String,
    val path: String,
    val parentPath: String? = null,
    val type: FolderType = FolderType.ALL,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 文件夹类型
 */
enum class FolderType {
    MUSIC,
    VIDEO,
    ALL;

    companion object {
        fun fromMimeType(mimeType: String): FolderType {
            return when {
                mimeType.startsWith("audio/") -> MUSIC
                mimeType.startsWith("video/") -> VIDEO
                else -> ALL
            }
        }
    }
}
