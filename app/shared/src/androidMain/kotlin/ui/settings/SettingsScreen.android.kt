package com.xmvisio.app.ui.settings

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xmvisio.app.update.UpdateViewModel

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

@Composable
actual fun rememberUpdateViewModel(): Any {
    val context = LocalContext.current
    return remember { UpdateViewModel(context) }
}
