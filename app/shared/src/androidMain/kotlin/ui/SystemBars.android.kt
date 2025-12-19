package com.xmvisio.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 配置系统栏颜色
 * 适配浅色/深色模式
 * 
 * 注意：这是系统栏配置的唯一入口点，MainActivity 不再单独配置
 */
@Composable
actual fun ConfigureSystemBars(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val activity = LocalContext.current as? ComponentActivity
    
    if (activity != null) {
        DisposableEffect(isDark, statusBarColor) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarConfigurator.createStatusBarStyle(
                    isDark = isDark,
                    statusBarColor = statusBarColor
                ),
                navigationBarStyle = SystemBarConfigurator.createNavigationBarStyle(isDark)
            )
            onDispose { }
        }
    }
}
