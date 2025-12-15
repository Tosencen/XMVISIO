package com.xmvisio.app.ui.foundation.text

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.TextStyle

@Composable
fun ProvideTextStyleContentColor(
    textStyle: TextStyle,
    color: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTextStyle provides textStyle,
        LocalContentColor provides color
    ) {
        content()
    }
}
