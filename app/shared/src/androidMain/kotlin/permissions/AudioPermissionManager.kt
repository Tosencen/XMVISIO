package com.xmvisio.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限状态
 */
enum class PermissionStatus {
    GRANTED,            // 已授予
    DENIED,             // 被拒绝
    PERMANENTLY_DENIED, // 永久拒绝
    NOT_REQUESTED       // 未请求
}

/**
 * 音频权限管理器
 */
class AudioPermissionManager(private val context: Context) {
    
    companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "audio_permissions"
        private const val KEY_HAS_REQUESTED = "has_requested_audio"
        
        /**
         * 获取所需权限
         */
        fun getRequiredPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }
    
    /**
     * 检查权限状态
     */
    fun checkPermissionStatus(): PermissionStatus {
        val permission = getRequiredPermission()
        
        return when {
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            context is Activity && 
                ActivityCompat.shouldShowRequestPermissionRationale(context, permission) -> {
                PermissionStatus.DENIED
            }
            else -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val hasRequested = prefs.getBoolean(KEY_HAS_REQUESTED, false)
                if (hasRequested) {
                    PermissionStatus.PERMANENTLY_DENIED
                } else {
                    PermissionStatus.NOT_REQUESTED
                }
            }
        }
    }
    
    /**
     * 请求权限
     */
    fun requestPermission(activity: Activity) {
        val permission = getRequiredPermission()
        
        // 记录已请求
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_REQUESTED, true).apply()
        
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            AUDIO_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            return grantResults.isNotEmpty() && 
                   grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        return false
    }
}
