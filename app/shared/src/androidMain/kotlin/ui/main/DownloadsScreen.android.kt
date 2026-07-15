/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
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
    updateAvailable: Boolean,
    onUpdateCheck: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> android.util.Log.d("DownloadsScreen", "Storage permission granted: $isGranted") }
    val downloadManager = remember {
        SealDownloadManager.getInstance(context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    SealDownloadScreen(downloadManager = downloadManager, updateAvailable = updateAvailable, onUpdateCheck = onUpdateCheck, modifier = modifier)
}
*/
