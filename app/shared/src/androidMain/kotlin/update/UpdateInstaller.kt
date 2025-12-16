package com.xmvisio.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * APK 安装器
 */
class UpdateInstaller(private val context: Context) {
    
    /**
     * 检查是否有安装权限
     */
    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
    
    /**
     * 请求安装权限
     */
    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * 安装 APK
     */
    fun installApk(apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) {
                Log.e("UpdateInstaller", "APK文件不存在: ${apkFile.absolutePath}")
                return false
            }
            
            // 检查安装权限
            if (!hasInstallPermission()) {
                Log.w("UpdateInstaller", "没有安装权限")
                requestInstallPermission()
                return false
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 使用 FileProvider
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    // Android 7.0 以下直接使用文件URI
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                }
            }
            
            context.startActivity(intent)
            Log.d("UpdateInstaller", "启动安装界面")
            true
            
        } catch (e: Exception) {
            Log.e("UpdateInstaller", "安装失败: ${e.message}", e)
            false
        }
    }
}
