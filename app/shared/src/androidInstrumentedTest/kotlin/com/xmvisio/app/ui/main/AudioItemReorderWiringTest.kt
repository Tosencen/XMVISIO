package com.xmvisio.app.ui.main

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xmvisio.app.audio.LocalAudioFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Duration.Companion.seconds

/**
 * 真实 [AudioItem] 组件（而非测试宿主）的排序拖拽 instrumented 测试。
 *
 * 目的：锁定 AudiobookScreenImpl 中 AudioItem 的**参数接线**——尤其是
 * `dragHandleModifier` 必须被应用到拖拽手柄上（曾因漏挂导致排序模式无法拖动）。
 *
 * 与 ReorderNoSnapBackTest 的区别：那个测试复刻了列表结构（测试宿主），
 * 本测试直接渲染生产代码里的 [AudioItem]，并传入与生产调用点相同的
 * `dragHandleModifier = Modifier.draggableHandle()`，只要未来有人在
 * AudioItem 内部漏挂 dragHandleModifier，本测试立即失败。
 * （已验证：注入漏挂 bug 后本测试 2 个用例全部失败，而宿主测试不受影响）
 */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(AndroidJUnit4::class)
class AudioItemReorderWiringTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun audio(id: Long) = LocalAudioFile(
        id = id,
        title = "Audio $id",
        artist = "Artist $id",
        duration = 3 * 60 * 1000L,
        uri = Uri.parse("content://test/audio/$id"),
        dateAdded = id,
        displayName = "Audio $id.mp3"
    )

    /**
     * 测试宿主：与生产调用点（AudiobookScreenImpl）保持一致的接线。
     * - 真实 [AudioItem]，`dragHandleModifier = Modifier.draggableHandle()`（ReorderableItem scope 内）
     * - 列表由 derivedStateOf 从 mutableStateMapOf 缓存派生（复刻 categorySortedListsCache）
     * - 拖拽回调使用真实纯函数 [moveItemInList]
     * - 列表项间距用 Arrangement.spacedBy（与生产一致），ReorderableItem 是列表项唯一内容
     *
     * @param initialItems 初始音频 id 列表
     * @param onOrderChange 每次拖拽后回调最新顺序（id 列表）
     */
    @Composable
    private fun AudioItemHarness(
        initialItems: List<Long>,
        onOrderChange: (List<Long>) -> Unit
    ) {
        val cache = remember {
            mutableStateMapOf<String, List<LocalAudioFile>>().apply {
                put("cat_test", initialItems.map(::audio))
            }
        }
        val items by remember {
            derivedStateOf { cache["cat_test"] ?: emptyList() }
        }

        val listState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            cache["cat_test"] = moveItemInList(items, from.index, to.index)
            onOrderChange(cache["cat_test"]?.map { it.id } ?: emptyList())
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().testTag("audio_reorder_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = items, key = { it.id }) { audio ->
                ReorderableItem(reorderableState, key = audio.id) { isDragging ->
                    // 与生产调用点一致的接线（仅 isReorderMode 固定 true 以显示拖拽手柄）
                    AudioItem(
                        audio = audio,
                        savedPosition = 0.seconds,
                        isThisAudioPlaying = false,
                        isReorderMode = true,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.draggableHandle(),
                        isBatchSelectionMode = false,
                        isSelected = false,
                        onCardClick = {},
                        onLongClick = {},
                        // 统一卡片 tag，供测量列表项高度（不依赖具体 id）
                        modifier = Modifier.testTag("audio_item_card")
                    )
                }
            }
        }
    }

    /**
     * 核心回归场景：真实 AudioItem 的拖拽手柄必须响应长按拖拽。
     * 若 dragHandleModifier 漏挂（如原 bug），手柄无手势，顺序不变，本测试失败。
     */
    @Test
    fun realAudioItem_dragHandle_wiresReorder() {
        var latestOrder: List<Long> = emptyList()
        composeRule.setContent {
            AudioItemHarness(initialItems = listOf(1L, 2L, 3L)) { newOrder ->
                latestOrder = newOrder
            }
        }
        composeRule.waitForIdle()

        // 把 id=3 的音频拖到顶部：3,1,2
        composeRule.onNodeWithTag("drag_handle_3", useUnmergedTree = true)
            .dragHandleBySteps(
                itemRef = composeRule.onAllNodesWithTag("audio_item_card", useUnmergedTree = true)[0],
                density = composeRule.density,
                deltaYPerStep = -0.6f
            )
        composeRule.waitForIdle()

        assertEquals(listOf(3L, 1L, 2L), latestOrder)
    }

    /**
     * 拖拽中间项到末尾，验证真实组件下顺序同样保持。
     */
    @Test
    fun realAudioItem_middleItemToEnd_keepsOrder() {
        var latestOrder: List<Long> = emptyList()
        composeRule.setContent {
            AudioItemHarness(initialItems = listOf(1L, 2L, 3L)) { newOrder ->
                latestOrder = newOrder
            }
        }
        composeRule.waitForIdle()

        // 把 id=2 的音频拖到末尾：1,3,2
        composeRule.onNodeWithTag("drag_handle_2", useUnmergedTree = true)
            .dragHandleBySteps(
                itemRef = composeRule.onAllNodesWithTag("audio_item_card", useUnmergedTree = true)[0],
                density = composeRule.density,
                deltaYPerStep = 0.6f
            )
        composeRule.waitForIdle()

        assertEquals(listOf(1L, 3L, 2L), latestOrder)
    }
}
