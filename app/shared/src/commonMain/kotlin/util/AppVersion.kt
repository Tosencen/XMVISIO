package com.xmvisio.app.util

import androidx.compose.runtime.Composable

/**
 * 获取应用版本号（Composable）
 */
@Composable
expect fun rememberAppVersion(): String
