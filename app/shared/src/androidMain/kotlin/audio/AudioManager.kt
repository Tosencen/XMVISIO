package com.xmvisio.app.audio

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频操作结果
 */
sealed class AudioOperationResult {
    data object Success : AudioOperationResult()
    data object Failed : AudioOperationResult()
    data class NeedPermission(val intentSender: IntentSender) : AudioOperationResult()
}

/**
 * 音频管理器
 */
class AudioManager(private val context: Context) {
    
    private var mediaService: com.xmvisio.app.media.LocalMediaService? = null
    
    /**
     * 初始化（需要在 Activity 中调用）
     */
    fun initialize(activity: androidx.activity.ComponentActivity) {
        if (mediaService == null) {
            mediaService = com.xmvisio.app.media.LocalMediaService(context).apply {
                initialize(activity)
            }
        }
    }
    
    private fun getMediaService(): com.xmvisio.app.media.LocalMediaService {
        return mediaService ?: throw IllegalStateException("AudioManager not initialized. Call initialize() first.")
    }
    
    /**
     * 重命名音频文件
     */
    suspend fun renameAudio(uri: Uri, newName: String): AudioOperationResult = withContext(Dispatchers.IO) {
        try {
            val success = getMediaService().renameMedia(uri, newName)
            if (success) {
                AudioOperationResult.Success
            } else {
                AudioOperationResult.Failed
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "重命名失败", e)
            AudioOperationResult.Failed
        }
    }
    
    /**
     * 删除音频文件
     */
    suspend fun deleteAudio(uri: Uri): AudioOperationResult = withContext(Dispatchers.IO) {
        try {
            val success = getMediaService().deleteMedia(listOf(uri))
            if (success) {
                AudioOperationResult.Success
            } else {
                AudioOperationResult.Failed
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "删除失败", e)
            AudioOperationResult.Failed
        }
    }
}
