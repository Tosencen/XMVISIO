package com.xmvisio.app.ui.main

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * 缓存逻辑测试：
 * - VersionedCache：ensureCategoryCountsLoaded 的版本键逻辑（版本变化重查、同版本复用、invalidate 强制重查）
 * - AudioPositionsCache：loadAudioPositions 的刷新时机（播放后刷新显示最新进度、无一次性守卫）
 */
class CacheLogicTest {

    // ==================================================================
    // VersionedCache（分类计数版本键逻辑）
    // ==================================================================

    @Test
    fun `loads only when version changes`() = runBlocking {
        var loadCount = 0
        val cache = VersionedCache<Int> {
            loadCount++
            loadCount
        }

        // 首次加载（version = 0）
        assertEquals(1, cache.get(0))
        assertEquals(1, loadCount)

        // 同版本再取 → 不重新加载
        assertEquals(1, cache.get(0))
        assertEquals(1, loadCount)

        // 版本变化（如批量移动音频后 mappingVersion++）→ 重新加载
        assertEquals(2, cache.get(1))
        assertEquals(2, loadCount)
    }

    @Test
    fun `needLoad reflects version changes`() {
        val cache = VersionedCache<Int> { 0 }
        assertTrue(cache.needLoad(0), "未加载时应该需要加载")
        runBlocking { cache.get(0) }
        assertFalse(cache.needLoad(0), "同版本不需要加载")
        assertTrue(cache.needLoad(1), "版本变化后需要加载")
    }

    @Test
    fun `invalidate forces reload even at same version`() = runBlocking {
        var value = 10
        val cache = VersionedCache<Int> { value }

        assertEquals(10, cache.get(0))
        // 数据源变化但版本号没变（分类增删改场景）→ invalidate 后必须重查
        value = 20
        cache.invalidate()
        assertTrue(cache.needLoad(0))
        assertEquals(20, cache.get(0))
    }

    @Test
    fun `counts scenario - move audio then reopen folder refreshes counts`() = runBlocking {
        // 模拟分类计数：以 mappingVersion 为 key，数据源随版本变化
        val countsByVersion = mapOf(
            0 to mapOf("cat_1" to 0),
            1 to mapOf("cat_1" to 3),
            2 to mapOf("cat_1" to 3, "cat_2" to 2)
        )
        var currentVersion = 0
        val cache = VersionedCache<Map<String, Int>> { countsByVersion.getValue(currentVersion) }

        // 首次进入文件夹主页
        assertEquals(mapOf("cat_1" to 0), cache.get(currentVersion))
        // 再次进入（无变化）→ 直接复用缓存
        assertEquals(mapOf("cat_1" to 0), cache.get(currentVersion))

        // 批量移动 3 个音频到 cat_1 → mappingVersion 变 1 → 重新查询
        currentVersion = 1
        assertEquals(mapOf("cat_1" to 3), cache.get(currentVersion))

        // 新增分类 cat_2（invalidate 触发重查）
        currentVersion = 2
        cache.invalidate()
        assertEquals(mapOf("cat_1" to 3, "cat_2" to 2), cache.get(currentVersion))
    }

    // ==================================================================
    // AudioPositionsCache（播放位置刷新时机）
    // ==================================================================

    @Test
    fun `get returns zero for unknown audio`() {
        val cache = AudioPositionsCache()
        assertEquals(kotlin.time.Duration.ZERO, cache.get(999L))
    }

    @Test
    fun `refresh unconditionally reloads - playback then rescan shows latest position`() = runBlocking {
        val cache = AudioPositionsCache()
        // 首次扫描：音频 1 播放到 30s
        cache.refresh { mapOf(1L to 30.seconds) }
        assertEquals(30.seconds, cache.get(1L))

        // 用户继续播放到 65s 后返回列表，触发重新扫描 → 必须显示最新进度（无一次性守卫）
        cache.refresh { mapOf(1L to 65.seconds) }
        assertEquals(65.seconds, cache.get(1L), "播放后重新扫描应显示最新进度，而非首次加载的旧值")
    }

    @Test
    fun `refresh replaces entire map - removed audio disappears`() = runBlocking {
        val cache = AudioPositionsCache()
        cache.refresh { mapOf(1L to 10.seconds, 2L to 20.seconds) }
        // 音频 1 被删除，只剩 2
        cache.refresh { mapOf(2L to 25.seconds) }
        assertEquals(kotlin.time.Duration.ZERO, cache.get(1L), "删除的音频位置应消失")
        assertEquals(25.seconds, cache.get(2L))
    }

    @Test
    fun `get falls back to zero after empty refresh`() = runBlocking {
        val cache = AudioPositionsCache()
        cache.refresh { mapOf(1L to 10.seconds) }
        cache.refresh { emptyMap() }
        assertEquals(kotlin.time.Duration.ZERO, cache.get(1L))
    }
}
