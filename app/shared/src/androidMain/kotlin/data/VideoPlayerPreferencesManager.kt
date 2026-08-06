package com.xmvisio.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoPlayerPreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("video_player_preferences", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<VideoPlayerPreferences> = _preferences.asStateFlow()

    companion object {
        // 播放
        private const val KEY_DEFAULT_SPEED = "default_playback_speed"
        private const val KEY_AUTOPLAY = "autoplay"
        private const val KEY_LOOP_MODE = "loop_mode"
        private const val KEY_RESUME_PLAYBACK = "resume_playback"

        // 手势
        private const val KEY_ENABLE_SEEK_GESTURE = "enable_seek_gesture"
        private const val KEY_SEEK_SENSITIVITY = "seek_sensitivity"
        private const val KEY_ENABLE_VOLUME_GESTURE = "enable_volume_gesture"
        private const val KEY_VOLUME_SENSITIVITY = "volume_sensitivity"
        private const val KEY_ENABLE_BRIGHTNESS_GESTURE = "enable_brightness_gesture"
        private const val KEY_BRIGHTNESS_SENSITIVITY = "brightness_sensitivity"
        private const val KEY_DOUBLE_TAP_GESTURE = "double_tap_gesture"
        private const val KEY_ENABLE_LONG_PRESS = "enable_long_press"
        private const val KEY_LONG_PRESS_SPEED = "long_press_speed"
        private const val KEY_SEEK_INCREMENT = "seek_increment"

        // 缩放手势
        private const val KEY_ENABLE_ZOOM_GESTURE = "enable_zoom_gesture"
        private const val KEY_ENABLE_PAN_GESTURE = "enable_pan_gesture"

        // 界面
        private const val KEY_AUTO_HIDE_TIMEOUT = "controller_auto_hide_timeout"
        private const val KEY_CONTROLS_LOCKED = "controls_locked"
        private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
        private const val KEY_VIDEO_CONTENT_SCALE = "video_content_scale"

        @Volatile
        private var instance: VideoPlayerPreferencesManager? = null

        fun getInstance(context: Context): VideoPlayerPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: VideoPlayerPreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private fun loadPreferences(): VideoPlayerPreferences {
        return VideoPlayerPreferences(
            defaultPlaybackSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f),
            autoplay = prefs.getBoolean(KEY_AUTOPLAY, true),
            loopMode = runCatching { LoopMode.valueOf(prefs.getString(KEY_LOOP_MODE, LoopMode.OFF.name)!!) }.getOrDefault(LoopMode.OFF),
            resumePlayback = prefs.getBoolean(KEY_RESUME_PLAYBACK, true),
            enableSeekGesture = prefs.getBoolean(KEY_ENABLE_SEEK_GESTURE, true),
            seekSensitivity = prefs.getFloat(KEY_SEEK_SENSITIVITY, 0.5f),
            enableVolumeGesture = prefs.getBoolean(KEY_ENABLE_VOLUME_GESTURE, true),
            volumeSensitivity = prefs.getFloat(KEY_VOLUME_SENSITIVITY, 0.5f),
            enableBrightnessGesture = prefs.getBoolean(KEY_ENABLE_BRIGHTNESS_GESTURE, true),
            brightnessSensitivity = prefs.getFloat(KEY_BRIGHTNESS_SENSITIVITY, 0.5f),
            doubleTapGesture = runCatching { DoubleTapGesture.valueOf(prefs.getString(KEY_DOUBLE_TAP_GESTURE, DoubleTapGesture.BOTH.name)!!) }.getOrDefault(DoubleTapGesture.BOTH),
            enableLongPress = prefs.getBoolean(KEY_ENABLE_LONG_PRESS, true),
            longPressSpeed = prefs.getFloat(KEY_LONG_PRESS_SPEED, 2.0f),
            seekIncrement = prefs.getInt(KEY_SEEK_INCREMENT, 10),
            enableZoomGesture = prefs.getBoolean(KEY_ENABLE_ZOOM_GESTURE, true),
            enablePanGesture = prefs.getBoolean(KEY_ENABLE_PAN_GESTURE, false),
            controllerAutoHideTimeout = prefs.getInt(KEY_AUTO_HIDE_TIMEOUT, 4),
            controlsLocked = prefs.getBoolean(KEY_CONTROLS_LOCKED, false),
            screenOrientation = runCatching { ScreenOrientation.valueOf(prefs.getString(KEY_SCREEN_ORIENTATION, ScreenOrientation.AUTOMATIC.name)!!) }.getOrDefault(ScreenOrientation.AUTOMATIC),
            videoContentScale = runCatching { VideoContentScale.valueOf(prefs.getString(KEY_VIDEO_CONTENT_SCALE, VideoContentScale.BEST_FIT.name)!!) }.getOrDefault(VideoContentScale.BEST_FIT),
        )
    }

    private fun update(block: (VideoPlayerPreferences) -> VideoPlayerPreferences) {
        val updated = block(_preferences.value)
        _preferences.value = updated
        savePreferences(updated)
    }

    private fun savePreferences(p: VideoPlayerPreferences) {
        prefs.edit().apply {
            putFloat(KEY_DEFAULT_SPEED, p.defaultPlaybackSpeed)
            putBoolean(KEY_AUTOPLAY, p.autoplay)
            putString(KEY_LOOP_MODE, p.loopMode.name)
            putBoolean(KEY_RESUME_PLAYBACK, p.resumePlayback)
            putBoolean(KEY_ENABLE_SEEK_GESTURE, p.enableSeekGesture)
            putFloat(KEY_SEEK_SENSITIVITY, p.seekSensitivity)
            putBoolean(KEY_ENABLE_VOLUME_GESTURE, p.enableVolumeGesture)
            putFloat(KEY_VOLUME_SENSITIVITY, p.volumeSensitivity)
            putBoolean(KEY_ENABLE_BRIGHTNESS_GESTURE, p.enableBrightnessGesture)
            putFloat(KEY_BRIGHTNESS_SENSITIVITY, p.brightnessSensitivity)
            putString(KEY_DOUBLE_TAP_GESTURE, p.doubleTapGesture.name)
            putBoolean(KEY_ENABLE_LONG_PRESS, p.enableLongPress)
            putFloat(KEY_LONG_PRESS_SPEED, p.longPressSpeed)
            putInt(KEY_SEEK_INCREMENT, p.seekIncrement)
            putBoolean(KEY_ENABLE_ZOOM_GESTURE, p.enableZoomGesture)
            putBoolean(KEY_ENABLE_PAN_GESTURE, p.enablePanGesture)
            putInt(KEY_AUTO_HIDE_TIMEOUT, p.controllerAutoHideTimeout)
            putBoolean(KEY_CONTROLS_LOCKED, p.controlsLocked)
            putString(KEY_SCREEN_ORIENTATION, p.screenOrientation.name)
            putString(KEY_VIDEO_CONTENT_SCALE, p.videoContentScale.name)
            apply()
        }
    }

    fun setDefaultPlaybackSpeed(speed: Float) = update { it.copy(defaultPlaybackSpeed = speed) }
    fun setAutoplay(enabled: Boolean) = update { it.copy(autoplay = enabled) }
    fun setLoopMode(mode: LoopMode) = update { it.copy(loopMode = mode) }
    fun setResumePlayback(enabled: Boolean) = update { it.copy(resumePlayback = enabled) }
    fun setEnableSeekGesture(enabled: Boolean) = update { it.copy(enableSeekGesture = enabled) }
    fun setSeekSensitivity(sensitivity: Float) = update { it.copy(seekSensitivity = sensitivity) }
    fun setEnableVolumeGesture(enabled: Boolean) = update { it.copy(enableVolumeGesture = enabled) }
    fun setVolumeSensitivity(sensitivity: Float) = update { it.copy(volumeSensitivity = sensitivity) }
    fun setEnableBrightnessGesture(enabled: Boolean) = update { it.copy(enableBrightnessGesture = enabled) }
    fun setBrightnessSensitivity(sensitivity: Float) = update { it.copy(brightnessSensitivity = sensitivity) }
    fun setDoubleTapGesture(gesture: DoubleTapGesture) = update { it.copy(doubleTapGesture = gesture) }
    fun setEnableLongPress(enabled: Boolean) = update { it.copy(enableLongPress = enabled) }
    fun setLongPressSpeed(speed: Float) = update { it.copy(longPressSpeed = speed) }
    fun setSeekIncrement(seconds: Int) = update { it.copy(seekIncrement = seconds.coerceIn(1, 60)) }
    fun setControllerAutoHideTimeout(seconds: Int) = update { it.copy(controllerAutoHideTimeout = seconds.coerceIn(1, 30)) }
    fun setControlsLocked(locked: Boolean) = update { it.copy(controlsLocked = locked) }
    fun setScreenOrientation(orientation: ScreenOrientation) = update { it.copy(screenOrientation = orientation) }
    fun setVideoContentScale(scale: VideoContentScale) = update { it.copy(videoContentScale = scale) }
    fun setEnableZoomGesture(enabled: Boolean) = update { it.copy(enableZoomGesture = enabled) }
    fun setEnablePanGesture(enabled: Boolean) = update { it.copy(enablePanGesture = enabled) }
}
