/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.text.ProvideContentColor

/**
 * Provide buttons to navigate horizontally. Effectively works on desktop.
 */
@Composable
fun HorizontalScrollControlScaffold(
    state: HorizontalScrollControlState,
    modifier: Modifier = Modifier,
    scrollLeftButton: @Composable () -> Unit = {
        HorizontalScrollControlDefaults.ScrollLeftButton()
    },
    scrollRightButton: @Composable () -> Unit = {
        HorizontalScrollControlDefaults.ScrollRightButton()
    },
    buttonShape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Exit) {
                            state.calculate(false)
                            continue
                        }
                        event.changes.firstOrNull()?.let { pointerInputChange ->
                            state.calculate(true)
                        }
                    }
                }
            },
    ) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = HorizontalScrollControlDefaults.ButtonMargin),
        ) {
            Crossfade(targetState = state.showLeftButton) { show ->
                if (show) {
                    Surface(
                        onClick = { state.scrollBackward() },
                        shape = buttonShape,
                        color = Color.Unspecified,
                        content = scrollLeftButton,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = HorizontalScrollControlDefaults.ButtonMargin),
        ) {
            Crossfade(targetState = state.showRightButton) { show ->
                if (show) {
                    Surface(
                        onClick = { state.scrollForward() },
                        shape = buttonShape,
                        color = Color.Unspecified,
                        content = scrollRightButton,
                    )
                }
            }
        }
    }
}

/**
 * Create a [HorizontalScrollControlState] that can be used to navigate horizontally
 *
 * @param scrollableState the incoming scrollable state. Use this to detect
 *      if the content can be scrolled, then finally determine the visibility of navigation button.
 * @param onClickScroll called when clicked navigation button.
 *      `step` is the scroll step, positive means scroll forward.
 *      You should handle actual scrolling in this lambda.
 * @see HorizontalScrollControlScaffold
 */
@Composable
fun rememberHorizontalScrollControlState(
    scrollableState: ScrollableState,
    onClickScroll: (direction: HorizontalScrollControlState.Direction) -> Unit,
): HorizontalScrollControlState {
    val onClickScrollUpdated by rememberUpdatedState(onClickScroll)
    return remember(scrollableState) {
        HorizontalScrollControlState(scrollableState) { onClickScrollUpdated(it) }
    }
}

@Stable
class HorizontalScrollControlState(
    private val scrollableState: ScrollableState,
    private val onClickScroll: (direction: Direction) -> Unit
) {
    var showLeftButton: Boolean by mutableStateOf(false)
        private set
    var showRightButton: Boolean by mutableStateOf(false)
        private set

    fun calculate(hovered: Boolean) {
        showLeftButton = hovered && scrollableState.canScrollBackward
        showRightButton = hovered && scrollableState.canScrollForward
    }

    fun scrollBackward() {
        onClickScroll(Direction.BACKWARD)
    }

    fun scrollForward() {
        onClickScroll(Direction.FORWARD)
    }

    enum class Direction { BACKWARD, FORWARD }
}

@Stable
object HorizontalScrollControlDefaults {
    val ButtonMargin = 12.dp
    val ButtonSize = 64.dp
    val ScrollStep = 200.dp

    @Composable
    fun ScrollLeftButton(
        modifier: Modifier = Modifier,
        contentDescription: String = "Scroll left"
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.7f),
        ) {
            ProvideContentColor(Color.White) {
                Box(
                    modifier = Modifier.size(ButtonSize).then(modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = contentDescription,
                    )
                }
            }
        }
    }

    @Composable
    fun ScrollRightButton(
        modifier: Modifier = Modifier,
        contentDescription: String = "Scroll right"
    ) {
        ScrollLeftButton(
            Modifier.rotate(180f).then(modifier),
            contentDescription = contentDescription,
        )
    }
}