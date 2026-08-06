package com.xmvisio.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 字幕样式偏好管理器
 * 负责保存与读取字幕样式设置（字体大小、加粗、背景、系统样式等）
 */
class SubtitlePreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("subtitle_preferences", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<SubtitlePreferences> = _preferences.asStateFlow()

    companion object {
        private const val KEY_USE_SYSTEM = "use_system_caption_style"
        private const val KEY_TEXT_SIZE = "subtitle_text_size"
        private const val KEY_TEXT_BOLD = "subtitle_text_bold"
        private const val KEY_BACKGROUND = "subtitle_background"
        private const val KEY_EMBEDDED = "apply_embedded_styles"

        @Volatile
        private var instance: SubtitlePreferencesManager? = null

        fun getInstance(context: Context): SubtitlePreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SubtitlePreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private fun loadPreferences(): SubtitlePreferences {
        return SubtitlePreferences(
            useSystemCaptionStyle = prefs.getBoolean(KEY_USE_SYSTEM, false),
            textSize = prefs.getInt(KEY_TEXT_SIZE, 20),
            textBold = prefs.getBoolean(KEY_TEXT_BOLD, false),
            background = prefs.getBoolean(KEY_BACKGROUND, true),
            applyEmbeddedStyles = prefs.getBoolean(KEY_EMBEDDED, true),
        )
    }

    private fun update(block: (SubtitlePreferences) -> SubtitlePreferences) {
        val updated = block(_preferences.value)
        _preferences.value = updated
        savePreferences(updated)
    }

    private fun savePreferences(p: SubtitlePreferences) {
        prefs.edit().apply {
            putBoolean(KEY_USE_SYSTEM, p.useSystemCaptionStyle)
            putInt(KEY_TEXT_SIZE, p.textSize)
            putBoolean(KEY_TEXT_BOLD, p.textBold)
            putBoolean(KEY_BACKGROUND, p.background)
            putBoolean(KEY_EMBEDDED, p.applyEmbeddedStyles)
            apply()
        }
    }

    fun setUseSystemCaptionStyle(enabled: Boolean) = update { it.copy(useSystemCaptionStyle = enabled) }
    fun setTextSize(size: Int) = update { it.copy(textSize = size.coerceIn(8, 60)) }
    fun setTextBold(enabled: Boolean) = update { it.copy(textBold = enabled) }
    fun setBackground(enabled: Boolean) = update { it.copy(background = enabled) }
    fun setApplyEmbeddedStyles(enabled: Boolean) = update { it.copy(applyEmbeddedStyles = enabled) }
}
