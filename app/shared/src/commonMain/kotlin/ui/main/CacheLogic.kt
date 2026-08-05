package com.xmvisio.app.ui.main

import kotlin.time.Duration

/**
 * 缓存相关纯逻辑（无 Android 依赖，可单元测试）
 *
 * 供 AudiobookScreenState 使用，把“按版本失效的缓存”和“播放位置缓存刷新时机”
 * 从 ViewModel 中提取出来，方便单测。
 */

/**
 * 按版本号失效的通用缓存。
 *
 * 只有 [version] 变化时才调用 [load] 重新加载，否则直接返回上次结果。
 * 用于分类计数等场景：分类/映射变化（mappingVersion 自增）时重查 DB，
 * 未变化时直接用缓存，避免重复查询。
 */
internal class VersionedCache<T>(
    private val load: suspend () -> T
) {
    private var loadedVersion: Int? = null
    private var cached: T? = null

    /**
     * 是否需要重新加载（与当前版本比较）。用于调用方在启动协程前快速判断，
     * 避免未变化时也启动一次协程。
     */
    fun needLoad(version: Int): Boolean = loadedVersion != version

    /**
     * 获取缓存值。
     * @param version 当前版本号；与上次加载时不同才重新加载
     */
    suspend fun get(version: Int): T {
        if (needLoad(version)) {
            cached = load()
            loadedVersion = version
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }

    /** 强制下次 [get] 重新加载（分类增删改时调用，即使版本号未变） */
    fun invalidate() {
        loadedVersion = null
    }
}

/**
 * 音频播放位置缓存。
 *
 * 关键语义：每次 [refresh] 都无条件重新加载（刻意不做一次性加载守卫）。
 * 因为用户播放过音频后位置会变化，返回列表时（触发重新扫描）必须显示最新进度，
 * 否则列表项进度会永远停留在首次加载时的值。
 */
internal class AudioPositionsCache {
    var positions: Map<Long, Duration> = emptyMap()
        private set

    /** 无条件重新加载（每次扫描后调用） */
    suspend fun refresh(load: suspend () -> Map<Long, Duration>) {
        positions = load()
    }

    fun get(audioId: Long): Duration = positions[audioId] ?: Duration.ZERO
}
