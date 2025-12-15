/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.saveable

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.structuralEqualityPolicy

fun <Content, Saveable : Any> mutableStateSaver(
    contentSaver: Saver<Content, Saveable>,
    policy: SnapshotMutationPolicy<Content> = structuralEqualityPolicy()
): Saver<MutableState<Content>, Saveable> {
    return Saver(
        save = { state ->
            with(contentSaver) {
                save(state.value)
            }
        },
        restore = { saveable ->
            contentSaver.restore(saveable)?.let {
                mutableStateOf(it, policy)
            }
        },
    )
}
