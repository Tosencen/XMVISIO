package com.xmvisio.app.ui.player

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView as MediaSubtitleView
import com.xmvisio.app.data.SubtitlePreferences

/**
 * 监听播放器的字幕 cues，返回当前需要显示的字幕列表。
 */
@UnstableApi
@Composable
private fun rememberCues(player: Player): List<Cue> {
    var cues by remember { mutableStateOf(player.currentCues.cues) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_CUES)) {
                    cues = player.currentCues.cues
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return cues
}

/**
 * 可自定义样式的字幕视图（参考 NextPlayer 思路）。
 * 叠加在视频画面之上，样式由 [SubtitlePreferences] 控制。
 */
@UnstableApi
@Composable
fun SubtitleView(
    player: Player,
    prefs: SubtitlePreferences,
    modifier: Modifier = Modifier,
) {
    val cues = rememberCues(player)
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            MediaSubtitleView(context).apply { applySubtitleStyle(this, context, prefs) }
        },
        update = { view ->
            view.setCues(cues)
            applySubtitleStyle(view, view.context, prefs)
        }
    )
}

private fun applySubtitleStyle(
    view: MediaSubtitleView,
    context: Context,
    prefs: SubtitlePreferences,
) {
    if (prefs.useSystemCaptionStyle) {
        val captioningManager =
            context.getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        if (captioningManager != null) {
            view.setStyle(CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle))
        }
    } else {
        val typeface = Typeface.create(
            Typeface.DEFAULT,
            if (prefs.textBold) Typeface.BOLD else Typeface.NORMAL
        )
        val style = CaptionStyleCompat(
            android.graphics.Color.WHITE,
            if (prefs.background) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            android.graphics.Color.BLACK,
            typeface
        )
        view.setStyle(style)
        view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSize.toFloat())
    }
    view.setApplyEmbeddedStyles(prefs.applyEmbeddedStyles)
}
