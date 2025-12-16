package com.xmvisio.app.util

import androidx.compose.runtime.Composable

/**
 * Desktop 平台获取应用版本号
 */
@Composable
actual fun rememberAppVersion(): String {
    return "1.0.3" // Desktop 版本（需要手动更新）
}
