package com.xmvisio.app.ui.settings

import androidx.compose.runtime.Composable

@Composable
expect fun PlaybackSettingsSection(
    onNavigateToTheme: () -> Unit,
    onNavigateToVideoPlayerSettings: () -> Unit,
    onNavigateToFolderPreferences: () -> Unit
)
