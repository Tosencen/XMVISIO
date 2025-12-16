package com.xmvisio.app.media

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * 本地媒体服务实现
 * 参考 nextplayer 项目实现
 */
class LocalMediaService(private val context: Context) : MediaService {
    
    private val contentResolver = context.contentResolver
    private var resultOkCallback: () -> Unit = {}
    private var resultCancelledCallback: () -> Unit = {}
    private var mediaRequestLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    
    override fun initialize(activity: ComponentActivity) {
        mediaRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> resultOkCallback()
                Activity.RESULT_CANCELED -> resultCancelledCallback()
            }
        }
    }
    
    override suspend fun deleteMedia(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteMediaR(uris)
        } else {
            deleteMediaBelowR(uris)
        }
    }
    
    override suspend fun renameMedia(uri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            renameMediaR(uri, newName)
        } else {
            renameMediaBelowR(uri, newName)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchDeleteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {}
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createDeleteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent).build())
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchWriteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {}
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createWriteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent).build())
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMediaR(uris: List<Uri>): Boolean = suspendCancellableCoroutine { continuation ->
        launchDeleteRequest(
            uris = uris,
            onResultOk = { continuation.resume(true) },
            onResultCanceled = { continuation.resume(false) }
        )
    }
    
    private suspend fun deleteMediaBelowR(uris: List<Uri>): Boolean {
        return uris.map { uri ->
            deleteMediaUri(uri)
        }.all { it }
    }
    
    private suspend fun deleteMediaUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun renameMediaR(uri: Uri, newName: String): Boolean = suspendCancellableCoroutine { continuation ->
        launchWriteRequest(
            uris = listOf(uri),
            onResultOk = {
                // 权限授予后，在 IO 线程中执行重命名
                CoroutineScope(Dispatchers.IO).launch {
                    val result = try {
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
                        }
                        val updateCount = contentResolver.update(uri, values, null, null)
                        android.util.Log.d("LocalMediaService", "重命名更新结果: $updateCount")
                        updateCount > 0
                    } catch (e: Exception) {
                        android.util.Log.e("LocalMediaService", "重命名失败", e)
                        false
                    }
                    continuation.resume(result)
                }
            },
            onResultCanceled = { 
                android.util.Log.d("LocalMediaService", "用户取消重命名")
                continuation.resume(false) 
            }
        )
    }
    
    private suspend fun renameMediaBelowR(uri: Uri, newName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            }
            contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            android.util.Log.e("LocalMediaService", "重命名失败", e)
            false
        }
    }
    

}
