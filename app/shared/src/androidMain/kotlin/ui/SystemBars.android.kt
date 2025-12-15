package com.xmvisio.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 配置系统栏颜色
 * 适配浅色/深色模式
 */
@Composable
actual fun ConfigureSystemBars(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val activity = LocalContext.current as? ComponentActivity
    
    if (activity != null) {
        DisposableEffect(activity, isDark) {
            if (isDark) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                )
            } else {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                    navigationBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                )
            }
            onDispose { }
        }
    }
}
