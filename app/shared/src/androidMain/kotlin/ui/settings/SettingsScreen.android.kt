package com.xmvisio.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xmvisio.app.update.UpdateViewModel

/**
 * Android 平台的更新对话框实现
 */
@Composable
actual fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
) {
    UpdateDialog(
        onDismiss = onDismiss,
        updateViewModel = updateViewModel as UpdateViewModel
    )
}

/**
 * Android 平台创建 UpdateViewModel
 */
@Composable
actual fun rememberUpdateViewModel(): Any {
    val context = LocalContext.current
    return remember { UpdateViewModel(context) }
}
