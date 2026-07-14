package com.xmvisio.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayerPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
