/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
package com.xmvisio.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun DownloadsScreen(
    updateAvailable: Boolean,
    onUpdateCheck: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("下载", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            Text("桌面端下载暂不支持", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
*/
