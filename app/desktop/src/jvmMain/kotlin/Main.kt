package com.xmvisio.app.desktop

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.xmvisio.app.App
import com.xmvisio.app.ui.LocalTitleBarInsets
import java.awt.Dimension

/**
 * Desktop 应用入口
 * 窗口尺寸参考 Animeko
 */
fun main() = application {
    // 设置 macOS 应用名称
    System.setProperty("apple.awt.application.name", "XMVISIO")
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "XMVISIO",
        state = WindowState(
            size = DpSize(960.dp, 680.dp),  // 默认尺寸：960 x 680
            position = WindowPosition.Aligned(Alignment.Center)
        ),
    ) {
        // 设置最小窗口尺寸 600 x 500，确保 NavigationRail 和设置按钮都能正常显示
        // 如果窗口太小，会切换到 NavigationBar，但设置按钮仍然可以通过导航项访问
        window.minimumSize = Dimension(600, 500)
        
        // 设置窗口背景色为黑色（防止闪眼）
        window.background = java.awt.Color.BLACK
        window.contentPane.background = java.awt.Color.BLACK
        
        // macOS: 设置透明标题栏（复刻 Animeko）
        val osName = System.getProperty("os.name").lowercase()
        val isMacOS = osName.contains("mac")
        
        if (isMacOS) {
            window.rootPane.putClientProperty("apple.awt.application.appearance", "system")
            window.rootPane.putClientProperty("apple.awt.fullscreenable", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        }
        
        // 为 macOS 标题栏留出空间
        CompositionLocalProvider(
            LocalTitleBarInsets provides if (isMacOS) {
                WindowInsets(top = 28.dp) // 为红绿黄按钮留出空间
            } else {
                WindowInsets(0, 0, 0, 0)
            }
        ) {
            App()
        }
    }
}
