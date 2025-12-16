package com.xmvisio.app

import androidx.compose.runtime.Composable

/**
 * Desktop 平台不需要更新检查，直接显示应用
 */
@Composable
actual fun AppWithUpdateCheck(updateViewModel: Any) {
    App()
}
