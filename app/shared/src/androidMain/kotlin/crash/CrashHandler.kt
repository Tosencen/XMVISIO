package com.xmvisio.app.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全局异常处理器
 * 捕获未处理的异常并显示崩溃页面
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    companion object {
        private const val TAG = "CrashHandler"
        const val CRASH_ERROR_KEY = "crash_error_message"
        const val CRASH_STACK_KEY = "crash_stack_trace"
        
        @Volatile
        private var instance: CrashHandler? = null
        
        fun init(context: Context) {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                    }
                }
            }
            Thread.setDefaultUncaughtExceptionHandler(instance)
        }
        
        /**
         * 全局 CoroutineExceptionHandler，捕获协程内未处理异常并转发到崩溃流程
         */
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            instance?.uncaughtException(Thread.currentThread(), throwable)
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 记录异常信息
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            // 获取异常信息
            val errorMessage = throwable.message ?: throwable::class.java.simpleName
            val stackTrace = getStackTraceString(throwable)
            
            // 保存崩溃信息到日志
            saveCrashLog(errorMessage, stackTrace)
            
            // 重启应用并显示崩溃页面
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CRASH_ERROR_KEY, errorMessage)
                putExtra(CRASH_STACK_KEY, stackTrace)
            }
            
            if (intent != null) {
                context.startActivity(intent)
            }
            
            // 延迟终止进程，让崩溃页面有机会渲染和显示
            Handler(Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(1)
            }, 3000) // 3 秒延迟，足够崩溃页展示
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in crash handler", e)
            // 如果崩溃处理失败，使用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 获取堆栈跟踪字符串
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
    
    /**
     * 保存崩溃日志
     */
    private fun saveCrashLog(errorMessage: String, stackTrace: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val deviceInfo = buildDeviceInfo()
            
            val crashLog = """
                ========== CRASH LOG ==========
                Time: $timestamp
                Error: $errorMessage
                
                Device Info:
                $deviceInfo
                
                Stack Trace:
                $stackTrace
                ===============================
            """.trimIndent()
            
            Log.e(TAG, crashLog)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }
    
    /**
     * 构建设备信息
     */
    private fun buildDeviceInfo(): String {
        return """
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Brand: ${Build.BRAND}
            Device: ${Build.DEVICE}
            Board: ${Build.BOARD}
        """.trimIndent()
    }
}

/**
 * 从 Intent 中提取崩溃信息
 */
fun Intent.getCrashInfo(): Pair<String?, String?> {
    val errorMessage = getStringExtra(CrashHandler.CRASH_ERROR_KEY)
    val stackTrace = getStringExtra(CrashHandler.CRASH_STACK_KEY)
    return errorMessage to stackTrace
}
