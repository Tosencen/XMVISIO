package com.xmvisio.app

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class XmvisioApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate() called")
        
        // 初始化 YoutubeDL 和 FFmpeg
        try {
            Log.d(TAG, "Starting YoutubeDL initialization...")
            YoutubeDL.getInstance().init(this)
            Log.d(TAG, "✓ YoutubeDL initialized successfully")
            
            Log.d(TAG, "Starting FFmpeg initialization...")
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "✓ FFmpeg initialized successfully")
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "✗ Failed to initialize YoutubeDL/FFmpeg", e)
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Unexpected error during initialization", e)
            e.printStackTrace()
        }
    }
    
    companion object {
        private const val TAG = "XmvisioApplication"
    }
}
