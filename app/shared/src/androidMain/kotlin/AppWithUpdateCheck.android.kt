package com.xmvisio.app

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xmvisio.app.update.UpdateViewModel
import com.xmvisio.app.update.UpdateState
import com.xmvisio.app.ui.settings.UpdateDialog

/**
 * 带自动更新检查的应用入口（Android）
 */
@Composable
actual fun AppWithUpdateCheck(updateViewModel: Any) {
    val vm = updateViewModel as UpdateViewModel
    val context = LocalContext.current
    val updateState by vm.updateState.collectAsState()
    
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
    
    // 启动时自动检查更新
    LaunchedEffect(Unit) {
        vm.checkUpdate(currentVersion)
    }
    
    // 显示应用主界面
    App()
    
    // 只在有新版本时显示更新对话框
    if (updateState is UpdateState.HasUpdate || 
        updateState is UpdateState.Downloading || 
        updateState is UpdateState.Downloaded ||
        updateState is UpdateState.DownloadFailed ||
        updateState is UpdateState.Installing ||
        updateState is UpdateState.InstallPermissionRequested ||
        updateState is UpdateState.InstallFailed) {
        UpdateDialog(
            onDismiss = { vm.reset() },
            updateViewModel = vm
        )
    }
}
