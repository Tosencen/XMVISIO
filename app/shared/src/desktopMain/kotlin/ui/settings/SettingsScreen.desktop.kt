package com.xmvisio.app.ui.settings

import androidx.compose.runtime.Composable

/**
 * Desktop 平台的更新对话框实现（暂不支持）
 */
@Composable
actual fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
) {
    // Desktop 平台暂不支持自动更新
}

/**
 * Desktop 平台创建 UpdateViewModel（暂不支持）
 */
@Composable
actual fun rememberUpdateViewModel(): Any {
    return object {} // 返回空对象
}

/**
 * Desktop 平台的下载更新实现（暂不支持）
 */
actual suspend fun startDownloadUpdate(
    downloadUrl: String,
    fallbackUrl: String?,
    version: String,
    onProgress: (Float) -> Unit,
    onResult: (UpdateState) -> Unit
) {
    // Desktop 平台暂不支持自动更新
    onResult(UpdateState.DownloadFailed("Desktop 平台暂不支持自动更新"))
}
