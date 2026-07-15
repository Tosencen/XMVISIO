package com.xmvisio.app.ui.player.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

/**
 * 自定义水平拖动手势检测器。
 * 使用 [awaitHorizontalTouchSlopOrCancellation] 确保只有水平方向的拖动才会触发，
 * 垂直拖动会被系统级 touch slop 排除，实现水平/垂直手势的互斥分离。
 *
 * @param onDragStart 拖动开始时回调，参数为手指初始位置
 * @param onDragEnd 拖动正常结束时回调
 * @param onDragCancel 拖动被取消时回调
 * @param onHorizontalDrag 水平拖动过程中的回调，change 为当前指针事件，dragAmount 为本次水平位移
 */
suspend fun PointerInputScope.detectCustomHorizontalDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var overSlop = 0f
        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            overSlop = over
        }
        // 仅单指触摸时处理
        if (drag != null && currentEvent.changes.count { it.pressed } == 1) {
            onDragStart.invoke(drag.position)
            onHorizontalDrag(drag, overSlop)
            if (
                horizontalDrag(drag.id) {
                    onHorizontalDrag(it, it.positionChange().x)
                    it.consume()
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * 自定义垂直拖动手势检测器。
 * 使用 [awaitVerticalTouchSlopOrCancellation] 确保只有垂直方向的拖动才会触发，
 * 水平拖动会被系统级 touch slop 排除，实现水平/垂直手势的互斥分离。
 *
 * @param onDragStart 拖动开始时回调，参数为手指初始位置（用于判断左右半屏）
 * @param onDragEnd 拖动正常结束时回调
 * @param onDragCancel 拖动被取消时回调
 * @param onVerticalDrag 垂直拖动过程中的回调，change 为当前指针事件，dragAmount 为本次垂直位移
 */
suspend fun PointerInputScope.detectCustomVerticalDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var overSlop = 0f
        val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            overSlop = over
        }
        // 仅单指触摸时处理
        if (drag != null && currentEvent.changes.count { it.pressed } == 1) {
            onDragStart.invoke(drag.position)
            onVerticalDrag.invoke(drag, overSlop)
            if (
                verticalDrag(drag.id) {
                    onVerticalDrag(it, it.positionChange().y)
                    it.consume()
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * 双指手势检测器：支持双指缩放（pinch zoom）和平移（pan）。
 * 参考 NextPlayer 的 detectCustomTransformGestures 实现。
 *
 * @param onTransform 双指变换回调，panChange 为平移量，zoomChange 为缩放倍率变化
 * @param onTransformEnd 手势结束时回调
 */
suspend fun PointerInputScope.detectCustomTransformGestures(
    onTransform: (panChange: Offset, zoomChange: Float) -> Unit,
    onTransformEnd: () -> Unit = {},
) {
    awaitEachGesture {
        var zoomChange = 0f
        var panChange = Offset.Zero
        // 等待至少两个手指按下
        val initialDown = awaitFirstDown(requireUnconsumed = false)
        while (currentEvent.changes.count { it.pressed } < 2) {
            val nextDown = awaitFirstDown(requireUnconsumed = false)
            if (nextDown == initialDown) continue
        }

        do {
            val event = awaitPointerEvent()
            val activeChanges = event.changes.filter { it.pressed }
            if (activeChanges.size < 2) break

            zoomChange = event.calculateZoom()
            panChange = event.calculatePan()

            if (zoomChange != 1f || panChange != Offset.Zero) {
                onTransform(panChange, zoomChange)
                // 消费所有活动变更
                activeChanges.forEach { it.consume() }
            }
        } while (activeChanges.size >= 2)

        onTransformEnd()
    }
}
