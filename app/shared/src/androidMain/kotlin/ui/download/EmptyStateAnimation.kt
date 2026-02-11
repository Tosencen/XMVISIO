package com.xmvisio.app.ui.download

import android.graphics.Color
import android.graphics.PorterDuff
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieDrawable
import com.materialkolor.ktx.toHct

@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val context = LocalContext.current
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

    val primaryHct = colorScheme.primary.toHct()
    val isDark = colorScheme.background.toHct().tone < 50.0

    val primaryColor = if (isDark) {
        Color.valueOf(
            Color.HSVToColor(
                floatArrayOf(
                    primaryHct.hue.toFloat(),
                    primaryHct.chroma.toFloat().coerceAtMost(35f),
                    65f
                )
            )
        )
    } else {
        Color.valueOf(
            Color.HSVToColor(
                floatArrayOf(
                    primaryHct.hue.toFloat(),
                    primaryHct.chroma.toFloat().coerceAtMost(30f),
                    45f
                )
            )
        )
    }

    AndroidView(
        factory = { ctx ->
            com.airbnb.lottie.LottieAnimationView(ctx).apply {
                setAnimation(ctx.resources.getIdentifier("empty_state", "raw", ctx.packageName))
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        },
        update = { view ->
            view.setColorFilter(primaryColor.toArgb(), PorterDuff.Mode.SRC_ATOP)
            view.invalidate()
        },
        modifier = modifier.size(size)
    )
}
