package com.xmvisio.app

/**
 * 应用常量配置
 */
object Constants {
    
    // 应用信息
    const val APP_NAME = "XMVISIO"
    
    // 外部链接
    const val GITHUB_URL = "https://github.com/Tosencen/XMVISIO"
    
    // API 配置
    const val GITHUB_API_BASE_URL = "https://api.github.com"
    const val CDN_BASE_URL = "https://cdn.jsdelivr.net"
    
    // 仓库信息
    const val GITHUB_OWNER = "Tosencen"
    const val GITHUB_REPO = "XMVISIO"
    
    // 更新检查
    const val UPDATE_CHECK_INTERVAL_HOURS = 24
    
    // SharedPreferences Keys
    object PrefsKeys {
        const val DARK_MODE = "dark_mode"
        const val SELECTED_COLOR = "selected_color"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val USE_BLACK_BACKGROUND = "use_black_background"
        const val LAST_UPDATE_CHECK = "last_update_check"
    }
}
