package com.xmvisio.app.ui.foundation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放动画 - 三根竖线动画
 * 参考XMSLEEP项目实现
 */
@Composable
fun PlayingAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 3
) {
    val bar1Height = remember { Animatable(0.5f) }
    val bar2Height = remember { Animatable(0.75f) }
    val bar3Height = remember { Animatable(1f) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            while (true) {
                // 第一根竖线动画
                bar1Height.animateTo(1f, animationSpec = tween(300, easing = EaseInOut))
                bar1Height.animateTo(0.5f, animationSpec = tween(300, easing = EaseInOut))
            }
        }
        
        scope.launch {
            delay(100)
            while (true) {
                // 第二根竖线动画
                bar2Height.animateTo(0.5f, animationSpec = tween(300, easing = EaseInOut))
                bar2Height.animateTo(1f, animationSpec = tween(300, easing = EaseInOut))
            }
        }
        
        scope.launch {
            delay(200)
            while (true) {
                // 第三根竖线动画
                bar3Height.animateTo(0.75f, animationSpec = tween(200, easing = EaseInOut))
                bar3Height.animateTo(0.25f, animationSpec = tween(200, easing = EaseInOut))
            }
        }
    }
    
    Canvas(modifier = modifier) {
        val totalBars = barCount
        val barSpacing = 3.dp.toPx() // 竖线之间的间距
        val barWidth = (size.width - barSpacing * (totalBars - 1)) / totalBars
        val minHeight = size.height * 0.25f
        val maxHeight = size.height
        
        // 绘制三根竖线，使用圆角矩形
        val heights = listOf(bar1Height.value, bar2Height.value, bar3Height.value)
        val cornerRadius = barWidth / 2 // 完全圆角
        
        for (i in 0 until barCount) {
            val x = i * (barWidth + barSpacing) + barWidth / 2
            val heightRatio = heights[i].coerceIn(0.25f, 1f)
            val barHeight = minHeight + (maxHeight - minHeight) * heightRatio
            val topY = size.height - barHeight
            
            // 绘制圆角矩形
            drawRoundRect(
                color = color,
                topLeft = Offset(x - barWidth / 2, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
