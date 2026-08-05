package com.xmvisio.app

import android.app.Application
import android.util.Log
import com.xmvisio.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// ===== 下载功能已暂时禁用 =====
// import com.yausername.youtubedl_android.YoutubeDL
// import com.yausername.youtubedl_android.YoutubeDLException

class XmvisioApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Application onCreate() called")

        // 后台预打开数据库（含 schema 迁移与旧数据导入），
        // 避免首次 UI 组合时在主线程同步打开/迁移导致卡顿
        appScope.launch {
            try {
                AppDatabase.getInstance(this@XmvisioApplication)
                Log.d(TAG, "AppDatabase initialized")
            } catch (t: Throwable) {
                Log.e(TAG, "AppDatabase 初始化失败", t)
            }
        }
        
        // ===== 下载功能已暂时禁用 =====
        // // 初始化 YoutubeDL
        // try {
        //     Log.d(TAG, "Starting YoutubeDL initialization...")
        //     YoutubeDL.getInstance().init(this)
        //     Log.d(TAG, "✓ YoutubeDL initialized successfully")
        // } catch (e: YoutubeDLException) {
        //     Log.e(TAG, "✗ Failed to initialize YoutubeDL", e)
        //     e.printStackTrace()
        // } catch (e: Exception) {
        //     Log.e(TAG, "✗ Unexpected error during initialization", e)
        //     e.printStackTrace()
        // } catch (t: Throwable) {
        //     // Keep app startup alive in release builds when native/reflection initialization fails.
        //     Log.e(TAG, "✗ Fatal error during initialization (continuing app startup)", t)
        // }
    }
    
    companion object {
        private const val TAG = "XmvisioApplication"
    }
}
