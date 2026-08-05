package com.xmvisio.app.ui.main

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 分类排序缓存逻辑测试。
 *
 * 覆盖 AudiobookScreenState 中 getCategorySortedList / updateCategorySortedList /
 * reorderInCategory 所依赖的纯逻辑（MediaListLogic.kt），以及通过模拟缓存 Map
 * 复现「音频移入后打开文件夹」「排序后不弹回」等真实场景。
 */
class CategoryCacheLogicTest {

    private data class Item(val id: Long, val name: String)

    private fun items(vararg pairs: Pair<Long, String>): List<Item> =
        pairs.map { (id, name) -> Item(id, name) }

    // ==================================================================
    // hasSameIdSet
    // ==================================================================

    @Test
    fun `same ids in different order is same set`() {
        val a = items(1L to "a", 2L to "b", 3L to "c")
        val b = items(3L to "c", 1L to "a", 2L to "b")
        assertTrue(hasSameIdSet(a, b) { it.id })
    }

    @Test
    fun `different size is not same set`() {
        val a = items(1L to "a", 2L to "b")
        val b = items(1L to "a", 2L to "b", 3L to "c")
        assertFalse(hasSameIdSet(a, b) { it.id })
    }

    @Test
    fun `different ids is not same set`() {
        val a = items(1L to "a", 2L to "b")
        val b = items(1L to "a", 9L to "x")
        assertFalse(hasSameIdSet(a, b) { it.id })
    }

    @Test
    fun `same order is same set`() {
        val a = items(1L to "a", 2L to "b")
        assertTrue(hasSameIdSet(a, a) { it.id })
    }

    @Test
    fun `two empty lists are same set`() {
        assertTrue(hasSameIdSet(emptyList<Item>(), emptyList<Item>()) { it.id })
    }

    @Test
    fun `empty vs non-empty is not same set`() {
        assertFalse(hasSameIdSet(emptyList<Item>(), items(1L to "a")) { it.id })
    }

    // ==================================================================
    // rebuildWithCachedOrder
    // ==================================================================

    @Test
    fun `rebuild keeps cached order but uses fresh objects`() {
        val cached = items(2L to "旧标题2", 1L to "旧标题1")
        // 模拟重新扫描后标题已更新
        val fresh = items(1L to "新标题1", 2L to "新标题2")
        val rebuilt = rebuildWithCachedOrder(cached, fresh) { it.id }
        assertEquals(listOf(2L, 1L), rebuilt.map { it.id }, "顺序应保持缓存顺序")
        assertEquals("新标题2", rebuilt[0].name, "对象应来自最新扫描")
        assertEquals("新标题1", rebuilt[1].name)
    }

    @Test
    fun `rebuild skips ids missing from base`() {
        val cached = items(2L to "b", 3L to "c", 1L to "a")
        val base = items(1L to "a", 2L to "b") // 3 已删除
        val rebuilt = rebuildWithCachedOrder(cached, base) { it.id }
        assertEquals(listOf(2L, 1L), rebuilt.map { it.id })
    }

    // ==================================================================
    // moveItemInList（排序重排）
    // ==================================================================

    @Test
    fun `move item backward`() {
        val list = items(1L to "a", 2L to "b", 3L to "c", 4L to "d")
        val moved = moveItemInList(list, 3, 0)
        assertEquals(listOf(4L, 1L, 2L, 3L), moved.map { it.id })
    }

    @Test
    fun `move item forward`() {
        val list = items(1L to "a", 2L to "b", 3L to "c", 4L to "d")
        val moved = moveItemInList(list, 0, 2)
        assertEquals(listOf(2L, 3L, 1L, 4L), moved.map { it.id })
    }

    @Test
    fun `move to same index returns unchanged`() {
        val list = items(1L to "a", 2L to "b")
        assertEquals(list, moveItemInList(list, 1, 1))
    }

    @Test
    fun `move with out of bounds returns original`() {
        val list = items(1L to "a", 2L to "b")
        assertEquals(list, moveItemInList(list, 5, 0))
        assertEquals(list, moveItemInList(list, 0, 9))
    }

    @Test
    fun `move to size index (drop at end) is ignored not crash`() {
        // 拖到列表末尾（toIndex == size）时旧代码会抛越界，新实现应安全忽略
        val list = items(1L to "a", 2L to "b", 3L to "c")
        assertEquals(list, moveItemInList(list, 0, list.size))
    }

    // ==================================================================
    // 场景模拟：复现 AudiobookScreenState 的缓存流程
    // ==================================================================

    /**
     * 模拟 State 的缓存 Map（key=categoryId, value=排序后列表）。
     * 复制 getCategorySortedList 的核心逻辑（纯函数组合）。
     */
    private class CacheSim {
        val cache = mutableMapOf<String, List<Item>>()

        fun getSortedList(categoryId: String, baseList: List<Item>, customOrder: List<Long>?): List<Item> {
            val cached = cache[categoryId]
            if (cached != null && hasSameIdSet(cached, baseList) { it.id }) {
                return rebuildWithCachedOrder(cached, baseList) { it.id }
            }
            return applyCustomOrderById(baseList, customOrder) { it.id }.also {
                cache[categoryId] = it
            }
        }

        fun reorder(categoryId: String, from: Int, to: Int) {
            val current = cache[categoryId] ?: return
            val newList = moveItemInList(current, from, to)
            cache[categoryId] = newList
            // saveOrder 不影响本测试
        }
    }

    @Test
    fun `audio moved into folder then opening folder shows it (not stale empty cache)`() {
        val sim = CacheSim()
        val categoryId = "cat_1"

        // 场景一：produceState 首次组合传入空 baseList → 空列表被缓存
        sim.getSortedList(categoryId, emptyList(), customOrder = null)
        assertEquals(emptyList<Item>(), sim.cache[categoryId])

        // 场景二：真实 ID 加载完成，baseList 现在包含音频 → 必须重算，不能返回缓存空列表
        val audios = items(1L to "音频A", 2L to "音频B")
        val result = sim.getSortedList(categoryId, audios, customOrder = null)
        assertEquals(listOf(1L, 2L), result.map { it.id }, "打开文件夹应显示音频内容")
        assertEquals(2, sim.cache[categoryId]?.size)
    }

    @Test
    fun `audio added to category appears after reopen`() {
        val sim = CacheSim()
        val categoryId = "cat_1"

        sim.getSortedList(categoryId, items(1L to "a"), customOrder = null)
        // 音频 2 移入该分类
        val result = sim.getSortedList(categoryId, items(1L to "a", 2L to "b"), customOrder = null)
        assertEquals(listOf(1L, 2L), result.map { it.id }, "新移入的音频应显示")
    }

    @Test
    fun `reorder does not snap back when reopening folder`() {
        val sim = CacheSim()
        val categoryId = "cat_1"
        val audios = items(1L to "a", 2L to "b", 3L to "c", 4L to "d")

        // 打开文件夹 → 缓存默认顺序
        sim.getSortedList(categoryId, audios, customOrder = null)

        // 排序模式：把第 1 项（a）拖到第 3 位
        sim.reorder(categoryId, 0, 2)
        assertEquals(listOf(2L, 3L, 1L, 4L), sim.cache[categoryId]?.map { it.id })

        // 重新打开文件夹（baseList 未变）→ 顺序保持，不弹回
        val reopened = sim.getSortedList(categoryId, audios, customOrder = null)
        assertEquals(listOf(2L, 3L, 1L, 4L), reopened.map { it.id }, "排序后重开不应弹回")
    }

    @Test
    fun `custom order is applied when cache is stale`() {
        val sim = CacheSim()
        val categoryId = "cat_1"

        // 自定义排序已保存（3, 1），缓存里是旧顺序
        sim.getSortedList(categoryId, items(1L to "a", 2L to "b", 3L to "c"), customOrder = null)
        // baseList 变化（音频 2 移出）→ 缓存失效 → 按 customOrder 重算
        val result = sim.getSortedList(categoryId, items(1L to "a", 3L to "c"), customOrder = listOf(3L, 1L))
        assertEquals(listOf(3L, 1L), result.map { it.id })
    }

    @Test
    fun `deleted audio disappears from cache after invalidation`() {
        val sim = CacheSim()
        val categoryId = "cat_1"

        sim.getSortedList(categoryId, items(1L to "a", 2L to "b", 3L to "c"), customOrder = null)
        // 删除音频 2 后，state 会把包含它的缓存条目移除（invalidateCategoryCachesContaining）
        sim.cache.remove(categoryId)
        // 重新打开 → 重算
        val result = sim.getSortedList(categoryId, items(1L to "a", 3L to "c"), customOrder = null)
        assertEquals(listOf(1L, 3L), result.map { it.id })
        assertEquals(2, result.size)
    }

    @Test
    fun `unchanged base list reuses cache without reordering`() {
        val sim = CacheSim()
        val categoryId = "cat_1"
        val audios = items(1L to "a", 2L to "b", 3L to "c")

        sim.getSortedList(categoryId, audios, customOrder = null)
        val result = sim.getSortedList(categoryId, audios, customOrder = null)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }
}
