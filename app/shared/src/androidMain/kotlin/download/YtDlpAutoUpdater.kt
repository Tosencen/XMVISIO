package com.xmvisio.app.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YtDlpAutoUpdater(private val context: Context) {
    private val prefs = context.getSharedPreferences("ytdlp_updater", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "YtDlpAutoUpdater"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val WEEK_IN_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7天
    }
    
    /**
     * 检查是否需要更新，如果距离上次检查超过一周则自动更新
     */
    fun checkAndUpdateIfNeeded(downloadManager: IDownloadManager) {
        scope.launch {
            try {
                val lastCheckTime = prefs.getLong(KEY_LAST_CHECK, 0)
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastCheckTime >= WEEK_IN_MILLIS) {
                    Log.d(TAG, "开始自动检查 yt-dlp 更新...")
                    
                    val status = withContext(Dispatchers.IO) {
                        downloadManager.updateYtDlp()
                    }
                    
                    when (status) {
                        YtDlpUpdateStatus.DONE -> {
                            Log.d(TAG, "yt-dlp 更新成功")
                        }
                        YtDlpUpdateStatus.ALREADY_UP_TO_DATE -> {
                            Log.d(TAG, "yt-dlp 已是最新版本")
                        }
                        else -> {
                            Log.w(TAG, "yt-dlp 更新失败")
                        }
                    }
                    
                    // 更新最后检查时间
                    prefs.edit().putLong(KEY_LAST_CHECK, currentTime).apply()
                } else {
                    val daysUntilNextCheck = (WEEK_IN_MILLIS - (currentTime - lastCheckTime)) / (24 * 60 * 60 * 1000L)
                    Log.d(TAG, "距离下次自动检查还有 $daysUntilNextCheck 天")
                }
            } catch (e: Exception) {
                Log.e(TAG, "自动更新检查失败", e)
            }
        }
    }
    
    /**
     * 手动重置检查时间（用于测试）
     */
    fun resetCheckTime() {
        prefs.edit().putLong(KEY_LAST_CHECK, 0).apply()
    }
}
