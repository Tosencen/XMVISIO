package me.him188.ani.app.ui.foundation.interaction

import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

actual fun Modifier.onClickEx(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean,
    onDoubleClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit
): Modifier {
    // 对于 Mac 平台，使用自定义的点击处理来绕过默认的 500ms 双击检测延迟
    // 这样可以立即响应单击，提升用户体验
    return if (!enabled) {
        this.indication(interactionSource, indication)
    } else if (onDoubleClick == null && onLongClick == null) {
        // 如果没有双击和长按需求，使用快速响应方案（立即响应）
        this.pointerInput(enabled, onClick) {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                val up = waitForUpOrCancellation(down.id)
                if (up != null && !up.isConsumed) {
                    onClick()
                }
            }
        }.indication(interactionSource, indication)
    } else {
        // 有双击或长按需求时，使用优化的延迟方案
        // 将双击检测延迟从默认的 500ms 缩短到 200ms，提升响应速度
        this.pointerInput(enabled, onDoubleClick, onLongClick, onClick) {
            var lastClickTime = 0L
            var pendingClickJob: Job? = null
            val doubleClickTimeout = 200L // 缩短双击检测超时时间（毫秒），从默认 500ms 减少到 200ms
            val longPressTimeout = 500L // 长按检测超时时间（毫秒）
            
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                var longPressTriggered = false
                var longPressJob: Job? = null
                
                // 检查长按
                if (onLongClick != null) {
                    longPressJob = launch {
                        delay(longPressTimeout)
                        if (isActive && !longPressTriggered) {
                            longPressTriggered = true
                            pendingClickJob?.cancel()
                            pendingClickJob = null
                            onLongClick()
                        }
                    }
                }
                
                // 等待指针抬起，但设置超时来检测长按
                val up = withTimeoutOrNull(longPressTimeout + 100) {
                    waitForUpOrCancellation(down.id)
                }
                
                longPressJob?.cancel()
                
                if (up != null && !up.isConsumed && !longPressTriggered) {
                    // 指针在长按超时前抬起了，这是一个普通点击
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastClick = currentTime - lastClickTime
                    
                    // 检查是否是双击
                    if (onDoubleClick != null && timeSinceLastClick < doubleClickTimeout && lastClickTime > 0) {
                        pendingClickJob?.cancel()
                        pendingClickJob = null
                        onDoubleClick()
                        lastClickTime = 0L
                    } else {
                        // 取消之前的待处理单击
                        pendingClickJob?.cancel()
                        // 延迟执行单击，等待可能的双击
                        pendingClickJob = launch {
                            delay(doubleClickTimeout)
                            if (isActive) {
                                onClick()
                            }
                        }
                        lastClickTime = currentTime
                    }
                } else {
                    // 取消待处理的单击
                    pendingClickJob?.cancel()
                }
            }
        }.indication(interactionSource, indication)
    }
}

/**
 * 仅在 PC 有效. 鼠标右键单击.
 */
actual fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = onClick(
    enabled = enabled,
    interactionSource = interactionSource,
    matcher = PointerMatcher.pointer(PointerType.Mouse, button = PointerButton.Secondary),
    onClick = onClick,
)
