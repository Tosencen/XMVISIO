/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoadResult

@OptIn(ExperimentalTextApi::class)
@Composable
actual fun rememberPlatformFontFamily(
    fontName: String?,
): PlatformFontFamily {
    if (fontName == null) return PlatformFontFamily(null)

    var resolvedFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver) {
        val fontFamily = FontFamily(fontName)
        resolvedFontFamily = runCatching {
            val result = fontFamilyResolver.resolve(fontFamily).value as FontLoadResult
            if (result.typeface == null || result.typeface?.familyName != fontName) {
                null
            } else {
                fontFamily
            }
        }.getOrNull()
    }

    return PlatformFontFamily(resolvedFontFamily)
}