/* ===== 下载功能已暂时禁用（解除注释以恢复） =====
package com.xmvisio.app.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YtDlpAutoUpdater(private val context: Context) {
    private val prefs = context.getSharedPreferences("ytdlp_updater", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "YtDlpAutoUpdater"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val WEEK_IN_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
    
    fun checkAndUpdateIfNeeded(downloadManager: IDownloadManager) {
        scope.launch {
            try {
                val lastCheckTime = prefs.getLong(KEY_LAST_CHECK, 0)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastCheckTime >= WEEK_IN_MILLIS) {
                    val status = withContext(Dispatchers.IO) { downloadManager.updateYtDlp() }
                    prefs.edit().putLong(KEY_LAST_CHECK, currentTime).apply()
                }
            } catch (e: Exception) { Log.e(TAG, "Auto update check failed", e) }
        }
    }
    
    fun resetCheckTime() { prefs.edit().putLong(KEY_LAST_CHECK, 0).apply() }
    fun close() { scope.cancel() }
}
*/
