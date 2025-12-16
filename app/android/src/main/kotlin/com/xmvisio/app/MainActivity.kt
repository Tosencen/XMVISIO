package com.xmvisio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xmvisio.app.crash.CrashHandler
import com.xmvisio.app.crash.getCrashInfo
import com.xmvisio.app.ui.crash.CrashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化崩溃处理器
        CrashHandler.init(this)
        
        // 初始化主题设置管理器
        com.xmvisio.app.data.initializeThemeSettingsManager(this)
        
        // 初始化全局音频管理器
        com.xmvisio.app.audio.GlobalAudioManager.initialize(this, this)
        
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
                App()
            }
        }
    }
}
