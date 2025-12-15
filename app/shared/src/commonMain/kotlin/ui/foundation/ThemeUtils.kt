package com.xmvisio.app.ui.foundation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Color.weaken(factor: Float = 0.5f): Color {
    return this.copy(alpha = this.alpha * factor)
}
