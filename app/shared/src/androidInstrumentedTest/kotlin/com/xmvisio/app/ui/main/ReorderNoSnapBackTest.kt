package com.xmvisio.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 排序「不弹回」的 instrumented UI 测试（跑在模拟器/真机上）。
 *
 * 复刻 AudiobookScreenImpl 分类排序列表的真实结构（非 mock）：
 * - [ReorderableItem] + `draggableHandle()`（库内部 longPressDraggable：先长按再拖动）
 * - 列表由 `derivedStateOf` 从 `mutableStateMapOf` 缓存派生（修复后：拖拽写缓存 → 自动重组）
 * - 拖拽回调使用真实的纯函数 [moveItemInList]
 *
 * 覆盖：长按拖拽后释放，顺序保持新顺序（不弹回原位）。
 * 该场景无法用 adb input 稳定模拟（longPressDraggable 需要真实触摸流），
 * 故用 ComposeTestRule 的 performTouchInput 驱动。
 */
@RunWith(AndroidJUnit4::class)
class ReorderNoSnapBackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * 测试宿主：复刻真实排序列表的数据流。
     * @param onOrderChange 每次拖拽后回调最新顺序（测试侧断言用）
     */
    @Composable
    private fun ReorderHarness(
        initialItems: List<String>,
        onOrderChange: (List<String>) -> Unit
    ) {
        // 复刻 categorySortedListsCache（mutableStateMapOf = 快照状态，写入自动触发重组）
        val cache = remember {
            mutableStateMapOf<String, List<String>>().apply { put("cat_test", initialItems) }
        }
        // 复刻 AudiobookScreenImpl 的 derivedStateOf 读取：写缓存后列表自动重排，不弹回
        val items by remember { derivedStateOf { cache["cat_test"] ?: emptyList() } }

        val listState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            // 复刻 reorderInCategory：真实纯函数移动 + 写回缓存
            cache["cat_test"] = moveItemInList(items, from.index, to.index)
            onOrderChange(cache["cat_test"] ?: emptyList())
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().testTag("reorder_list")) {
            items(items = items, key = { it }) { item ->
                ReorderableItem(reorderableState, key = item) {
                    // draggableHandle() 是 ReorderableItem scope 的成员方法（内部 longPressDraggable）
                    // 所有 item 共用 "item_row" tag（同高），避免按内容 tag 查高度时的耦合
                    Row(modifier = Modifier.fillMaxWidth().testTag("item_row")) {
                        Text(item)
                        Spacer(Modifier.width(240.dp))
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "拖动排序",
                            modifier = Modifier.testTag("handle_$item").draggableHandle()
                        )
                    }
                }
            }
        }
    }

    /**
     * 场景一：长按拖拽 C 到顶部，释放后顺序保持 C,A,B（不弹回）。
     * 复现原始 bug：拖拽写缓存后列表弹回原位。
     */
    @Test
    fun reorderDrag_keepsNewOrder_afterRelease() {
        var latestOrder: List<String> = emptyList()
        composeRule.setContent {
            ReorderHarness(initialItems = listOf("A", "B", "C")) { newOrder ->
                latestOrder = newOrder
            }
        }
        composeRule.waitForIdle()

        // 长按 C 的拖拽手柄，向上拖过 2 个 item 高度（距离按实际 bounds 计算，避免密度依赖），再释放
        composeRule.onNodeWithTag("handle_C").dragHandleBySteps(
            itemRef = composeRule.onAllNodesWithTag("item_row")[0],
            density = composeRule.density,
            deltaYPerStep = -0.6f
        )
        composeRule.waitForIdle()

        // 核心断言：释放后顺序保持新顺序（无弹回）
        assertEquals(listOf("C", "A", "B"), latestOrder)
    }

    /**
     * 场景二：拖动中间项 B 到末尾，释放后顺序保持 A,C,B。
     */
    @Test
    fun reorderDrag_middleItemToEnd_keepsOrder() {
        var latestOrder: List<String> = emptyList()
        composeRule.setContent {
            ReorderHarness(initialItems = listOf("A", "B", "C")) { newOrder ->
                latestOrder = newOrder
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("handle_B").dragHandleBySteps(
            itemRef = composeRule.onAllNodesWithTag("item_row")[0],
            density = composeRule.density,
            deltaYPerStep = 0.6f
        )
        composeRule.waitForIdle()

        assertEquals(listOf("A", "C", "B"), latestOrder)
    }

    /**
     * 场景三：连续两次拖拽，缓存持续更新，累计顺序正确（验证不弹回非一次性）。
     */
    @Test
    fun consecutiveDrags_accumulateOrder() {
        var latestOrder: List<String> = emptyList()
        composeRule.setContent {
            ReorderHarness(initialItems = listOf("P", "Q", "R")) { newOrder ->
                latestOrder = newOrder
            }
        }
        composeRule.waitForIdle()

        // 第一次：R 拖到顶部 → R,P,Q
        composeRule.onNodeWithTag("handle_R").dragHandleBySteps(
            itemRef = composeRule.onAllNodesWithTag("item_row")[0],
            density = composeRule.density,
            deltaYPerStep = -0.6f
        )
        composeRule.waitForIdle()
        assertEquals(listOf("R", "P", "Q"), latestOrder)

        // 第二次：Q 拖到顶部 → Q,R,P（基于第一次的结果继续拖，验证缓存未弹回）
        composeRule.onNodeWithTag("handle_Q").dragHandleBySteps(
            itemRef = composeRule.onAllNodesWithTag("item_row")[0],
            density = composeRule.density,
            deltaYPerStep = -0.6f
        )
        composeRule.waitForIdle()
        assertEquals(listOf("Q", "R", "P"), latestOrder)
    }
}
