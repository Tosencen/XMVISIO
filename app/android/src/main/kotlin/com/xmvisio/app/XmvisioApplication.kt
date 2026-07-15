package com.xmvisio.app

import android.app.Application
import android.util.Log
// ===== 下载功能已暂时禁用 =====
// import com.yausername.youtubedl_android.YoutubeDL
// import com.yausername.youtubedl_android.YoutubeDLException

class XmvisioApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate() called")
        
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
