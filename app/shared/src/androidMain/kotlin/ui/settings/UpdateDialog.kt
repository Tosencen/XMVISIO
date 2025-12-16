package com.xmvisio.app.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xmvisio.app.update.UpdateViewModel
import com.xmvisio.app.update.UpdateState

/**
 * 软件更新对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    updateViewModel: UpdateViewModel
) {
    val context = LocalContext.current
    val updateState by updateViewModel.updateState.collectAsState()
    val downloadProgress by updateViewModel.downloadProgress.collectAsState()
    
    // 获取当前版本
    val currentVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    // 根据状态显示不同的内容
    when (val state = updateState) {
        is UpdateState.Idle -> {
            // 初始状态，检查更新
            LaunchedEffect(Unit) {
                updateViewModel.checkUpdate(currentVersion)
            }
            
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
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "当前已是最新版本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "当前版本：v$currentVersion",
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
            val version = state.version
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
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "版本：v${version.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "当前版本：v$currentVersion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        if (version.changelog.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "更新内容",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    version.changelog,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateViewModel.startDownload()
                        }
                    ) {
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
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            state.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "请检查网络连接后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateViewModel.reset()
                            updateViewModel.checkUpdate(currentVersion)
                        }
                    ) {
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
        
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* 下载中不允许关闭 */ },
                title = { Text("正在下载") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateViewModel.cancelDownload()
                            onDismiss()
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("下载完成") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("新版本已下载完成，点击安装")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateViewModel.installApk(state.file)
                        }
                    ) {
                        Text("立即安装")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            )
        }
        
        is UpdateState.DownloadFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("下载失败") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(state.error)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateViewModel.reset()
                            updateViewModel.startDownload()
                        }
                    ) {
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
        
        is UpdateState.Installing -> {
            AlertDialog(
                onDismissRequest = { /* 安装中不允许关闭 */ },
                title = { Text("正在安装") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("正在安装新版本...")
                    }
                },
                confirmButton = {}
            )
        }
        
        is UpdateState.InstallPermissionRequested -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("需要安装权限") },
                text = {
                    Text("请在设置中允许安装未知来源的应用")
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        
        is UpdateState.InstallFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("安装失败") },
                text = {
                    Text(state.error)
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
    }
}
