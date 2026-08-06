package com.xmvisio.app.data

/**
 * 媒体视图模式
 */
enum class MediaViewMode {
    FOLDER_TREE,  // 树形目录模式：同时显示子文件夹和当前层的媒体
    FOLDERS,      // 扁平文件夹模式：根目录只显示文件夹，进入后只显示媒体
    VIDEOS;       // 全部媒体模式：不区分文件夹，全部显示

    companion object {
        fun fromIndex(index: Int): MediaViewMode {
            return entries.getOrElse(index) { FOLDER_TREE }
        }
    }
}

/**
 * 排序方式
 */
enum class SortBy {
    NAME,   // 按名称
    DATE,   // 按日期
    SIZE;   // 按大小

    companion object {
        fun fromIndex(index: Int): SortBy {
            return entries.getOrElse(index) { DATE }
        }
    }
}

/**
 * 排序方向
 */
enum class SortOrder {
    DESCENDING, // 降序
    ASCENDING;  // 升序

    companion object {
        fun fromIndex(index: Int): SortOrder {
            return entries.getOrElse(index) { DESCENDING }
        }
    }
}
