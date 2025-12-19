package com.xmvisio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.xmvisio.app.crash.CrashHandler
import com.xmvisio.app.crash.getCrashInfo
import com.xmvisio.app.ui.crash.CrashScreen
import com.xmvisio.app.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    private var updateViewModel: UpdateViewModel? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 请求通知权限（Android 13+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        
        // 初始化崩溃处理器
        CrashHandler.init(this)
        
        // 初始化主题设置管理器
        com.xmvisio.app.data.initializeThemeSettingsManager(this)
        
        // 初始化全局音频管理器
        com.xmvisio.app.audio.GlobalAudioManager.initialize(this, this)
        
        // 初始化 URL Opener
        com.xmvisio.app.util.initializeUrlOpener(this)
        
        // 初始化更新 ViewModel
        updateViewModel = UpdateViewModel(this)
        
        // 检查是否有崩溃信息
        val (errorMessage, stackTrace) = intent.getCrashInfo()
        
        setContent {
            if (errorMessage != null && stackTrace != null) {
                // 显示崩溃页面
                CrashScreen(
                    errorMessage = errorMessage,
                    stackTrace = stackTrace,
                    onRestart = {
                        // 重启应用
                        finishAffinity()
                        startActivity(intent.apply {
                            removeExtra(CrashHandler.CRASH_ERROR_KEY)
                            removeExtra(CrashHandler.CRASH_STACK_KEY)
                        })
                    }
                )
            } else {
                // 正常显示应用
                AppWithUpdateCheck(updateViewModel = updateViewModel!!)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 检查是否从授权页面返回，如果已下载完成则自动安装
        updateViewModel?.checkAndInstallIfReady()
    }
}
