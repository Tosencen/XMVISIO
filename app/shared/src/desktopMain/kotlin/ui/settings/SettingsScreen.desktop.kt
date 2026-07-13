package com.xmvisio.app.ui.settings

import androidx.compose.runtime.Composable

@Composable
actual fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
) {
}

@Composable
actual fun rememberUpdateViewModel(): Any {
    return object {}
}
