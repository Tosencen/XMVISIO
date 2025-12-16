package com.xmvisio.app.data

import kotlinx.coroutines.flow.Flow

/**
 * 主题设置管理器接口
 */
interface ThemeSettingsManager {
    /**
     * 获取主题设置 Flow
     */
    val themeSettings: Flow<ThemeSettings>
    
    /**
     * 保存主题设置
     */
    suspend fun saveThemeSettings(settings: ThemeSettings)
    
    /**
     * 获取当前主题设置（同步方法）
     */
    suspend fun getThemeSettings(): ThemeSettings
}

/**
 * 创建平台特定的主题设置管理器
 */
expect fun createThemeSettingsManager(): ThemeSettingsManager
