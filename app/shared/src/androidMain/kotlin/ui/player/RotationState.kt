package com.xmvisio.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import com.xmvisio.app.data.ScreenOrientation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 屏幕方向状态管理
 *
 * 封装方向设置、恢复、手动旋转 toggle、视频尺寸响应等逻辑。
 */
class RotationState(
    private val player: Player,
    private val screenOrientation: ScreenOrientation,
) {
    private val originalOrientation = mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

    /** 根据用户偏好将 ScreenOrientation 映射为 Android ActivityInfo 常量 */
    private fun toActivityOrientation(orientation: ScreenOrientation): Int = when (orientation) {
        ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientation.VIDEO_ORIENTATION -> getVideoBasedOrientation()
    }

    /** 根据视频尺寸判断方向：竖屏视频 → 竖屏，横屏视频 → 横屏 */
    private fun getVideoBasedOrientation(): Int {
        val vs = player.videoSize
        if (vs.width == 0 || vs.height == 0) return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        return if (vs.height > vs.width) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    /** 进入播放页面时调用：记录原始方向并设置偏好方向 */
    fun applyOrientation(activity: Activity?) {
        val act = activity ?: return
        originalOrientation.intValue = act.requestedOrientation
        if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            act.requestedOrientation = toActivityOrientation(screenOrientation)
        }
    }

    /** 退出播放页面时调用：恢复原始方向 */
    fun restoreOrientation(activity: Activity?) {
        activity?.requestedOrientation = originalOrientation.intValue
    }

    /**
     * 手动旋转按钮：toggle 横屏 / 竖屏
     * 不锁定方向，设备传感器仍可响应旋转。
     */
    fun rotate(activity: Activity?) {
        val act = activity ?: return
        act.requestedOrientation = when (act.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    /**
     * 监听视频尺寸变化，当 [ScreenOrientation.VIDEO_ORIENTATION] 时动态调整方向。
     * 应在 LaunchedEffect 中调用。
     */
    suspend fun observeVideoSize(activity: Activity?) {
        if (screenOrientation != ScreenOrientation.VIDEO_ORIENTATION) return
        val act = activity ?: return
        var lastWidth = 0
        var lastHeight = 0
        while (currentCoroutineContext().isActive) {
            val vs = player.videoSize
            if (vs.width > 0 && vs.height > 0 &&
                (vs.width != lastWidth || vs.height != lastHeight)) {
                lastWidth = vs.width
                lastHeight = vs.height
                act.requestedOrientation = getVideoBasedOrientation()
            }
            delay(500)
        }
    }
}

/**
 * 创建并记住一个 [RotationState] 实例。
 */
@Composable
fun rememberRotationState(
    player: Player,
    screenOrientation: ScreenOrientation,
): RotationState {
    val currentPlayer by rememberUpdatedState(player)
    val currentOrientation by rememberUpdatedState(screenOrientation)
    return remember {
        RotationState(currentPlayer, currentOrientation)
    }
}
