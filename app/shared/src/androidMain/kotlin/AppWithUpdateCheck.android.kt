package com.xmvisio.app

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xmvisio.app.data.createThemeSettingsManager
import com.xmvisio.app.data.ThemeSettings
import com.xmvisio.app.update.UpdateState
import com.xmvisio.app.update.UpdateViewModel
import com.xmvisio.app.ui.settings.UpdateDialog
import com.xmvisio.app.ui.theme.AppTheme

/**
 * 带静默更新检查的应用入口（Android）
 * 有新版本时不弹窗，只显示更新图标，用户点击后才弹出更新对话框
 */
@Composable
actual fun AppWithUpdateCheck(updateViewModel: Any, openPlayerAudioId: Long?) {
    val vm = updateViewModel as UpdateViewModel
    val context = LocalContext.current
    val updateState by vm.updateState.collectAsState()

    val themeSettingsManager = remember { createThemeSettingsManager() }
    val themeSettings by produceState<ThemeSettings?>(initialValue = null) {
        themeSettingsManager.themeSettings.collect { settings ->
            value = settings
        }
    }

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

    var showUpdateDialog by remember { mutableStateOf(false) }

    // 启动时静默检查更新（1 小时内只检查一次）
    LaunchedEffect(Unit) {
        vm.startAutomaticCheckLatestVersion(currentVersion)
    }

    // 有更新/已下载/下载失败时显示更新图标
    val hasUpdate = remember(updateState) {
        updateState is UpdateState.HasUpdate ||
        updateState is UpdateState.Downloaded ||
        updateState is UpdateState.Downloading ||
        updateState is UpdateState.DownloadFailed
    }

    App(
        openPlayerAudioId = openPlayerAudioId,
        updateAvailable = hasUpdate,
        onUpdateCheck = { showUpdateDialog = true }
    )

    // 用户点击后才弹出更新对话框
    if (showUpdateDialog) {
        val currentThemeSettings = themeSettings ?: return
        AppTheme(themeSettings = currentThemeSettings) {
            UpdateDialog(
                onDismiss = {
                    showUpdateDialog = false
                    vm.reset()
                },
                updateViewModel = vm
            )
        }
    }
}
