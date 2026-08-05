package com.xmvisio.app.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.advanceEventTime
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Density

/**
 * 排序拖拽 instrumented 测试的共享辅助函数。
 *
 * 在接收者（已通过 onNodeWithTag 选中的拖拽手柄）上长按并分步拖动
 * [deltaYPerStep]×itemHeight，再释放。距离按 [itemRef] 指向的列表项
 * 实际高度自适应（跨 2 格绰绰有余），不同密度/分辨率下均稳定。
 */
fun SemanticsNodeInteraction.dragHandleBySteps(
    itemRef: SemanticsNodeInteraction,
    density: Density,
    deltaYPerStep: Float
) {
    val bounds = itemRef.getUnclippedBoundsInRoot()
    val itemHeight = with(density) { (bounds.bottom - bounds.top).toPx() }
    val step = itemHeight * deltaYPerStep
    this.performTouchInput {
        down(center)
        // 1200ms 超过 reorderable 库 longPressTimeoutMillis（默认 500ms），确保触发长按拖拽
        advanceEventTime(1200)
        repeat(6) {
            moveBy(Offset(0f, step), delayMillis = 16)
        }
        up()
    }
}
