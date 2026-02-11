package com.xmvisio.app.ui.download

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieDrawable

@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            com.airbnb.lottie.LottieAnimationView(ctx).apply {
                setAnimation(ctx.resources.getIdentifier("empty_state", "raw", ctx.packageName))
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        },
        update = { view ->
            view.invalidate()
        },
        modifier = modifier.size(size)
    )
}
