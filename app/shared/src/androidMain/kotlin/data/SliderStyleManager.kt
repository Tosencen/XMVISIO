package com.xmvisio.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放进度条样式
 */
enum class SliderStyle {
    DEFAULT,    // 默认样式（标准滑块）
    SQUIGGLY,   // 波浪样式（播放时有波浪动画）
    SLIM        // 纤细样式（无滑块）
}

/**
 * 播放进度条样式管理器
 */
class SliderStyleManager private constructor(context: Context) {
    
    private val prefs = context.getSharedPreferences("slider_settings", Context.MODE_PRIVATE)
    
    private val _sliderStyle = MutableStateFlow(getSliderStyle())
    val sliderStyle: StateFlow<SliderStyle> = _sliderStyle.asStateFlow()
    
    companion object {
        private const val KEY_SLIDER_STYLE = "slider_style"
        
        @Volatile
        private var instance: SliderStyleManager? = null
        
        fun getInstance(context: Context): SliderStyleManager {
            return instance ?: synchronized(this) {
                instance ?: SliderStyleManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 获取当前进度条样式
     */
    private fun getSliderStyle(): SliderStyle {
        val styleName = prefs.getString(KEY_SLIDER_STYLE, SliderStyle.DEFAULT.name)
        return try {
            SliderStyle.valueOf(styleName ?: SliderStyle.DEFAULT.name)
        } catch (e: IllegalArgumentException) {
            SliderStyle.DEFAULT
        }
    }
    
    /**
     * 设置进度条样式
     */
    fun setSliderStyle(style: SliderStyle) {
        prefs.edit().putString(KEY_SLIDER_STYLE, style.name).apply()
        _sliderStyle.value = style
    }
    
    /**
     * 获取样式的显示名称
     */
    fun getStyleDisplayName(style: SliderStyle): String {
        return when (style) {
            SliderStyle.DEFAULT -> "默认"
            SliderStyle.SQUIGGLY -> "波浪"
            SliderStyle.SLIM -> "纤细"
        }
    }
    
    /**
     * 获取样式的描述
     */
    fun getStyleDescription(style: SliderStyle): String {
        return when (style) {
            SliderStyle.DEFAULT -> "标准样式，带圆形滑块"
            SliderStyle.SQUIGGLY -> "播放时显示波浪动画"
            SliderStyle.SLIM -> "简洁样式，无滑块"
        }
    }
}
