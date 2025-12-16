package com.xmvisio.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 设置主页
 * 参考 XMSLEEP 的设计风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToTheme: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    val currentVersion = com.xmvisio.app.util.getAppVersion()
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
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            
            // 播放设置
            Text(
                text = "播放",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            PlaybackSettingsSection()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 外观设置
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsCard(
                icon = Icons.Filled.Palette,
                title = "主题与色彩",
                subtitle = "外观模式、主题色",
                onClick = onNavigateToTheme,
                showChevron = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 其他
            Text(
                text = "其他",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 软件更新
            SettingsCard(
                icon = Icons.Filled.SystemUpdate,
                title = "软件更新",
                subtitle = "检查并更新到最新版本",
                onClick = { showUpdateDialog = true },
                showChevron = false,
                trailingText = "v$currentVersion"
            )
            
            // 关于 XMVISIO
            SettingsCard(
                icon = Icons.Filled.Info,
                title = "关于 XMVISIO",
                subtitle = "查看应用信息、版本、版权",
                onClick = { /* TODO: 打开关于页面 */ },
                showChevron = false,
                trailingText = null
            )
        }
        
        // 更新检查对话框
        if (showUpdateDialog) {
            ShowUpdateDialog(
                updateViewModel = updateViewModel,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

/**
 * 显示更新对话框（平台特定实现）
 */
@Composable
expect fun ShowUpdateDialog(
    updateViewModel: Any,
    onDismiss: () -> Unit
)

/**
 * 创建 UpdateViewModel（平台特定实现）
 */
@Composable
expect fun rememberUpdateViewModel(): Any

/**
 * 设置卡片组件
 */
@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    showChevron: Boolean = true,
    trailingText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Column {
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
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
