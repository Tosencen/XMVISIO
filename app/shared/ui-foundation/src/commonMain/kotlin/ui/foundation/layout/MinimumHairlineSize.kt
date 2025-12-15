/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.layout

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Stable
private val MinimumHairlineSize = Modifier.sizeIn(
    minHeight = Dp.Hairline,// 保证最小大小, 否则 LazyColumn 滑动可能有 bug
    minWidth = Dp.Hairline,
)

@Stable
fun Modifier.minimumHairlineSize() = this then MinimumHairlineSize
