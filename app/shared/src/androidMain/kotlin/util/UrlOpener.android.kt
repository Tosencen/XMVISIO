package com.xmvisio.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

private var appContext: Context? = null

/**
 * 初始化 URL Opener（在 MainActivity 中调用）
 */
fun initializeUrlOpener(context: Context) {
    appContext = context.applicationContext
}

/**
 * Android 平台打开 URL
 */
actual fun openUrl(url: String) {
    val context = appContext ?: return
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
