package com.xmvisio.app.util

import java.awt.Desktop
import java.net.URI

/**
 * Desktop 平台打开 URL
 */
actual fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
