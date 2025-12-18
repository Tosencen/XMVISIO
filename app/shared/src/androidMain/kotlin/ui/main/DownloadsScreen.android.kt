package com.xmvisio.app.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xmvisio.app.download.SealDownloadManager
import com.xmvisio.app.ui.download.SealDownloadScreen

@Composable
actual fun DownloadsScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限结果会在 hasStoragePermission() 中自动反映
        android.util.Log.d("DownloadsScreen", "Storage permission granted: $isGranted")
    }
    
    val downloadManager = remember {
        SealDownloadManager.getInstance(context) {
            // 请求存储权限
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Android 12 及以下需要 WRITE_EXTERNAL_STORAGE
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                // Android 13+ 不需要权限，直接使用 MediaStore API
                android.util.Log.d("DownloadsScreen", "Android 13+, no permission needed")
            }
        }
    }
    
    SealDownloadScreen(
        downloadManager = downloadManager,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
    )
}
