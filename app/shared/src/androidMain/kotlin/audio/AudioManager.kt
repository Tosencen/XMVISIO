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
    
    /**
     * 重命名音频文件
     */
    suspend fun renameAudio(uri: Uri, newName: String): AudioOperationResult = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            }
            
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            if (rowsUpdated > 0) {
                AudioOperationResult.Success
            } else {
                AudioOperationResult.Failed
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = e as? RecoverableSecurityException
                    ?: throw RuntimeException(e.message, e)
                AudioOperationResult.NeedPermission(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            } else {
                android.util.Log.e("AudioManager", "重命名失败", e)
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
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                AudioOperationResult.Success
            } else {
                AudioOperationResult.Failed
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = e as? RecoverableSecurityException
                    ?: throw RuntimeException(e.message, e)
                AudioOperationResult.NeedPermission(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            } else {
                android.util.Log.e("AudioManager", "删除失败", e)
                AudioOperationResult.Failed
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "删除失败", e)
            AudioOperationResult.Failed
        }
    }
}
