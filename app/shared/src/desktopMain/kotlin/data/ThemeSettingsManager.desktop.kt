package com.xmvisio.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop 平台的主题设置管理器实现（简单实现）
 */
class DesktopThemeSettingsManager : ThemeSettingsManager {
    
    private val _themeSettings = MutableStateFlow(ThemeSettings.Default)
    
    override val themeSettings: Flow<ThemeSettings> = _themeSettings.asStateFlow()
    
    override suspend fun saveThemeSettings(settings: ThemeSettings) {
        _themeSettings.value = settings
    }
    
    override suspend fun getThemeSettings(): ThemeSettings {
        return _themeSettings.value
    }
}

/**
 * 创建 Desktop 平台的主题设置管理器
 */
actual fun createThemeSettingsManager(): ThemeSettingsManager {
    return DesktopThemeSettingsManager()
}
