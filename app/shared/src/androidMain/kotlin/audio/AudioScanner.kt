package com.xmvisio.app.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地音频文件
 */
data class LocalAudioFile(
    val id: Long,
    val title: String,
    val artist: String?,
    val duration: Long,
    val uri: Uri,
    val dateAdded: Long,
    val displayName: String
)

/**
 * 音频扫描器
 */
class AudioScanner(private val context: Context) {
    
    /**
     * 扫描所有音频文件
     */
    suspend fun scanAudioFiles(): List<LocalAudioFile> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<LocalAudioFile>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        
        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val title = displayName.substringBeforeLast(".")
                    val artist = cursor.getString(artistColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    
                    audioFiles.add(
                        LocalAudioFile(
                            id = id,
                            title = title,
                            artist = artist,
                            duration = duration,
                            uri = uri,
                            dateAdded = dateAdded,
                            displayName = displayName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioScanner", "扫描音频文件失败", e)
        }
        
        audioFiles
    }
}
