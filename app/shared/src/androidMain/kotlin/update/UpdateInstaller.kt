package com.xmvisio.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

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
            
            // 签名校验：确认待安装 APK 与当前安装的应用使用同一签名证书，
            // 防止下载源被攻破后安装被篡改/替换的 APK
            if (!verifyApkSignature(apkFile)) {
                Log.e("UpdateInstaller", "APK 签名校验失败，拒绝安装: ${apkFile.absolutePath}")
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
    
    /**
     * 校验 APK 签名证书与当前已安装应用的签名证书是否一致
     *
     * 原理：分别取出「当前安装应用」与「待安装 APK」的签名证书，
     * 计算 SHA-256 指纹后比对。任一已安装证书与 APK 证书匹配即通过
     * （兼容多证书签名场景）。
     *
     * @return true 表示签名一致（可信），false 表示无法校验或签名不一致
     */
    fun verifyApkSignature(apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            
            // 1. 当前已安装应用的签名证书
            val installedCerts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners?.map { it.toByteArray() }
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    .signatures?.map { it.toByteArray() }
            }
            
            // 2. 待安装 APK 的签名证书（解析 APK 文件本身，无需安装）
            val apkCerts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                    ?.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                    ?.signatures?.map { it.toByteArray() }
            }
            
            if (installedCerts.isNullOrEmpty() || apkCerts.isNullOrEmpty()) {
                Log.e(
                    "UpdateInstaller",
                    "无法获取签名信息：已安装证书=${installedCerts?.size}, APK证书=${apkCerts?.size}"
                )
                return false
            }
            
            val apkCertHashes = apkCerts.map { sha256Hex(it) }.toSet()
            val matched = installedCerts.any { sha256Hex(it) in apkCertHashes }
            if (!matched) {
                Log.e("UpdateInstaller", "APK 签名证书与当前应用不一致，已拒绝安装")
            } else {
                Log.d("UpdateInstaller", "APK 签名校验通过")
            }
            matched
            
        } catch (e: Exception) {
            Log.e("UpdateInstaller", "签名校验异常: ${e.message}", e)
            false
        }
    }
    
    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
