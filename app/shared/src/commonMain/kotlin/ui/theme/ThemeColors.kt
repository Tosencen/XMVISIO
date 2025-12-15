package com.xmvisio.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.xmvisio.app.data.DefaultSeedColor

/**
 * 基于 HCT 色彩空间生成颜色
 * 
 * HCT 色彩空间参数：
 * - Hue (色相): 0-360，决定颜色类型（红橙黄绿青蓝紫）
 * - Chroma (色度): 饱和度，40 是适中的饱和度
 * - Tone (色调): 明度，40 是中等明度
 */
private fun hctColor(hue: Double): Color =
    Color(Hct.from(hue, 40.0, 40.0).toInt())

/**
 * 12 种预定义主题颜色，色相按 30° 均匀分布，避免过近难以区分。
 * 默认紫色（DefaultSeedColor）放在 270° 段位置，保持品牌色同时保证区分度。
 */
val ThemeColorOptions: List<Color> = listOf(
    hctColor(0.0),    // 红
    hctColor(30.0),   // 橙
    hctColor(60.0),   // 黄
    hctColor(90.0),   // 黄绿
    hctColor(120.0),  // 绿
    hctColor(150.0),  // 翠绿
    hctColor(180.0),  // 青
    hctColor(210.0),  // 蓝
    hctColor(240.0),  // 靛
    DefaultSeedColor, // 品牌紫（约 270° 段）
    hctColor(300.0),  // 品红
    hctColor(330.0),  // 粉
)
