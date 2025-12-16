package com.xmvisio.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Android 平台的主题设置管理器实现
 */
class AndroidThemeSettingsManager(private val context: Context) : ThemeSettingsManager {
    
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "theme_settings")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val USE_DYNAMIC_THEME_KEY = booleanPreferencesKey("use_dynamic_theme")
        private val USE_BLACK_BACKGROUND_KEY = booleanPreferencesKey("use_black_background")
        private val SEED_COLOR_KEY = longPreferencesKey("seed_color")
    }
    
    override val themeSettings: Flow<ThemeSettings> = context.dataStore.data.map { preferences ->
        ThemeSettings(
            darkMode = when (preferences[DARK_MODE_KEY]) {
                "LIGHT" -> DarkMode.LIGHT
                "DARK" -> DarkMode.DARK
                else -> DarkMode.AUTO
            },
            useDynamicTheme = preferences[USE_DYNAMIC_THEME_KEY] ?: false,
            useBlackBackground = preferences[USE_BLACK_BACKGROUND_KEY] ?: false,
            seedColorValue = (preferences[SEED_COLOR_KEY] ?: DefaultSeedColor.value.toLong()).toULong()
        )
    }
    
    override suspend fun saveThemeSettings(settings: ThemeSettings) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = settings.darkMode.name
            preferences[USE_DYNAMIC_THEME_KEY] = settings.useDynamicTheme
            preferences[USE_BLACK_BACKGROUND_KEY] = settings.useBlackBackground
            preferences[SEED_COLOR_KEY] = settings.seedColorValue.toLong()
        }
    }
    
    override suspend fun getThemeSettings(): ThemeSettings {
        return themeSettings.first()
    }
}

/**
 * 创建 Android 平台的主题设置管理器
 */
actual fun createThemeSettingsManager(): ThemeSettingsManager {
    // 需要从 Application Context 获取
    return AndroidThemeSettingsManager(getApplicationContext())
}

/**
 * 获取 Application Context
 * 需要在 MainActivity 中初始化
 */
private var applicationContext: Context? = null

fun initializeThemeSettingsManager(context: Context) {
    applicationContext = context.applicationContext
}

private fun getApplicationContext(): Context {
    return applicationContext ?: throw IllegalStateException(
        "ThemeSettingsManager not initialized. Call initializeThemeSettingsManager() in MainActivity.onCreate()"
    )
}
