package com.xmvisio.app.ui.main

/**
 * 媒体列表的通用纯逻辑（无 Android 依赖，可单元测试）
 *
 * 供 AudiobookScreen 等页面使用，从 AudiobookScreenState 中提取。
 */

/**
 * 搜索匹配：标题或艺术家包含查询词（忽略大小写）。
 * query 为空时返回 true（不筛选）。
 */
internal fun matchesMediaQuery(title: String, artist: String?, query: String): Boolean {
    if (query.isBlank()) return true
    return title.contains(query, ignoreCase = true) ||
        artist?.contains(query, ignoreCase = true) == true
}

/**
 * 应用自定义排序：按 [customOrder] 中的 id 顺序重排，
 * 未出现在 customOrder 中的元素保持原相对顺序排在末尾。
 * [customOrder] 为 null 时直接返回原列表。
 */
internal fun <T> applyCustomOrderById(
    baseList: List<T>,
    customOrder: List<Long>?,
    idOf: (T) -> Long
): List<T> {
    if (customOrder == null) return baseList
    val orderedList = mutableListOf<T>()
    customOrder.forEach { id ->
        baseList.find { idOf(it) == id }?.let { orderedList.add(it) }
    }
    baseList.forEach { item ->
        if (idOf(item) !in customOrder) orderedList.add(item)
    }
    return orderedList
}

/**
 * 判断两个列表是否包含完全相同的 ID 集合（顺序可不同）。
 *
 * 用于分类排序缓存复用校验：只有 ID 集合一致时缓存才安全复用，
 * 否则（音频移入/移出/删除）必须重算，避免缓存返回过期内容。
 */
internal fun <T> hasSameIdSet(a: List<T>, b: List<T>, idOf: (T) -> Long): Boolean {
    if (a.size != b.size) return false
    val aIds = a.map(idOf).toSet()
    return b.all { idOf(it) in aIds }
}

/**
 * 用 [baseList] 的最新对象按 [cached] 的顺序重建列表。
 * 调用方需保证 ID 集合一致（见 [hasSameIdSet]）。
 * 这样缓存里只保留“顺序”，对象数据始终来自最新扫描结果（标题/时长刷新后能正确显示）。
 */
internal fun <T> rebuildWithCachedOrder(cached: List<T>, baseList: List<T>, idOf: (T) -> Long): List<T> {
    val baseById = baseList.associateBy(idOf)
    return cached.mapNotNull { baseById[idOf(it)] }
}

/**
 * 在列表中把 [fromIndex] 的元素移动到 [toIndex]（用于排序模式拖拽）。
 * 与原列表无交集或越界时返回原列表。
 */
internal fun <T> moveItemInList(list: List<T>, fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex) return list
    if (fromIndex !in list.indices || toIndex !in list.indices) return list
    val newList = list.toMutableList()
    val item = newList.removeAt(fromIndex)
    newList.add(toIndex, item)
    return newList
}
