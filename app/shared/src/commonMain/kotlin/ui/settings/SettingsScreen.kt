package com.xmvisio.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToTheme: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val currentVersion = com.xmvisio.app.util.rememberAppVersion()
    val updateViewModel = rememberUpdateViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // 播放与外观
            Text(
                text = "播放与外观",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
            )

            SettingsCardGroup {
                PlaybackSettingsSection(
                    onNavigateToTheme = onNavigateToTheme
                )
            }

            // 其他
            Text(
                text = "其他",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
            )

            SettingsCardGroup {
                SettingsCardItem(
                    icon = Icons.Filled.SystemUpdate,
                    title = "软件更新",
                    subtitle = "检查并更新到最新版本",
                    trailingText = "v$currentVersion",
                    onClick = { showUpdateDialog = true },
                    isFirst = true
                )

                SettingsCardItem(
                    icon = Icons.Filled.Feedback,
                    title = "功能反馈",
                    subtitle = "提交问题、建议或反馈",
                    onClick = {
                        com.xmvisio.app.util.openUrl("https://github.com/Tosencen/XMVISIO/issues")
                    }
                )

                SettingsCardItem(
                    icon = Icons.Filled.Info,
                    title = "关于 XMVISIO",
                    subtitle = "查看应用信息、版本、版权",
                    onClick = { showAboutDialog = true },
                    isLast = true
                )
            }
        }

        if (showUpdateDialog) {
            ShowUpdateDialog(
                updateViewModel = updateViewModel,
                onDismiss = { showUpdateDialog = false }
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                currentVersion = currentVersion,
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsCardGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
internal fun SettingsCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    trailingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val shape = if (isFirst && isLast) {
        RoundedCornerShape(24.dp)
    } else if (isFirst) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    } else if (isLast) {
        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    } else {
        RoundedCornerShape(6.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        SettingsCardRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            showChevron = showChevron,
            trailingText = trailingText,
            trailing = trailing,
            onClick = onClick
        )
    }
}

@Composable
internal fun SettingsCardRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    trailingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailing != null) {
            trailing()
        } else if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
expect fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
)

@Composable
expect fun rememberUpdateViewModel(): Any
