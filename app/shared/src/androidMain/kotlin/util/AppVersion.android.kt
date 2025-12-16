package com.xmvisio.app.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android 平台获取应用版本号
 */
actual fun getAppVersion(): String {
    // 这个函数需要 Context，所以我们提供一个 Composable 版本
    return "1.0.1" // 临时方案，从 gradle.properties 读取
}

/**
 * Composable 函数获取应用版本号
 */
@Composable
fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
