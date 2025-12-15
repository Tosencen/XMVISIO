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
    val currentVersion = "1.0.0"
    
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
            UpdateCheckDialog(
                currentVersion = currentVersion,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

/**
 * 更新检查对话框
 * 复刻 XMSLEEP 的设计
 */
@Composable
private fun UpdateCheckDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    
    // 模拟检查更新
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        updateState = UpdateState.UpToDate // 当前已是最新版本
        // updateState = UpdateState.HasUpdate("1.1.0", "## 更新内容\n\n- 修复了若干问题\n- 优化了性能\n- 新增了功能")
    }
    
    when (val state = updateState) {
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("软件更新") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("正在检查更新...")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("软件更新") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "当前已是最新版本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Text(
                            text = "当前版本：v$currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        
        is UpdateState.HasUpdate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("发现新版本") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "版本：v${state.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "当前版本：v$currentVersion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        if (state.changelog.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "更新内容",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = state.changelog,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { /* TODO: 下载更新 */ }) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            )
        }
        
        is UpdateState.CheckFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("检查更新失败") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "请检查网络连接后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        updateState = UpdateState.Checking
                        // 重新检查
                    }) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 更新状态
 */
private sealed class UpdateState {
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class HasUpdate(val version: String, val changelog: String) : UpdateState()
    data class CheckFailed(val error: String) : UpdateState()
}

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
