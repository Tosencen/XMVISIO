package com.xmvisio.app.folder

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.data.FolderType
import com.xmvisio.app.data.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * 文件夹扫描器
 * 负责扫描指定路径下的媒体文件
 */
class FolderScanner(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val contentResolver = context.contentResolver

    companion object {
        @Volatile
        private var instance: FolderScanner? = null

        fun getInstance(context: Context): FolderScanner {
            return instance ?: synchronized(this) {
                instance ?: FolderScanner(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * 扫描指定路径下的视频文件
     */
    suspend fun scanVideosInFolder(folderPath: String): List<VideoInfo> {
        return queryMediaStore(
            collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            },
            path = folderPath,
            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
            ),
            mapper = { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: return@queryMediaStore null
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: ""
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
                val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)).takeIf { it > 0 } ?: 0
                val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)).takeIf { it > 0 } ?: 0
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                VideoInfo(
                    id = id,
                    uri = uri,
                    name = name,
                    duration = duration,
                    size = size,
                    dateModified = dateModified,
                    path = path,
                    width = width,
                    height = height
                )
            }
        )
    }

    /**
     * 扫描指定路径下的音频文件
     */
    suspend fun scanAudiosInFolder(folderPath: String): List<LocalAudioFile> {
        return queryMediaStore(
            collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            },
            path = folderPath,
            projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
            ),
            mapper = { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)) ?: return@queryMediaStore null
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: ""
                val title = displayName.substringBeforeLast(".")
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                LocalAudioFile(
                    id = id,
                    title = title,
                    artist = artist,
                    duration = duration,
                    uri = uri,
                    dateAdded = dateAdded,
                    displayName = displayName
                )
            }
        )
    }

    /**
     * 获取文件夹内的子文件夹
     */
    fun getSubFolders(folderPath: String): List<String> {
        val dir = File(folderPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.absolutePath }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * 获取文件夹的修改时间
     */
    fun getFolderLastModified(folderPath: String): Long {
        return File(folderPath).lastModified()
    }

    /**
     * 检查路径是否存在
     */
    fun pathExists(path: String): Boolean {
        return File(path).exists()
    }

    /**
     * 获取路径类型（音频/视频/混合）
     */
    suspend fun getPathType(path: String): FolderType {
        val videos = scanVideosInFolder(path)
        val audios = scanAudiosInFolder(path)

        val hasVideos = videos.isNotEmpty()
        val hasAudios = audios.isNotEmpty()

        return when {
            hasVideos && hasAudios -> FolderType.ALL
            hasVideos -> FolderType.VIDEO
            hasAudios -> FolderType.MUSIC
            else -> FolderType.ALL
        }
    }

    /**
     * 通用的 MediaStore 查询方法
     */
    private suspend fun <T> queryMediaStore(
        collection: Uri,
        path: String,
        projection: Array<String>,
        mapper: (android.database.Cursor) -> T?
    ): List<T> {
        val results = mutableListOf<T>()

        // 使用 DATA 列进行路径过滤
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("${path}%")

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    mapper(cursor)?.let { result ->
                        // 只包含直接在该文件夹下的文件
                        // 检查文件的父路径是否匹配
                        if (result is VideoInfo) {
                            val filePath = result.path
                            val parentPath = File(filePath).parent
                            if (parentPath == path) {
                                results.add(result)
                            }
                        } else if (result is LocalAudioFile) {
                            val filePath = result.uri.path ?: ""
                            // 对于音频，我们通过 MediaStore 查询并检查路径
                            // 由于 LocalAudioFile 没有 path 字段，我们需要额外处理
                            results.add(result)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FolderScanner", "扫描失败: $path", e)
        }

        return results
    }

    /**
     * 通过路径过滤媒体文件
     */
    private fun filterByPath(mediaPath: String, targetPath: String): Boolean {
        val file = File(mediaPath)
        return file.parent == targetPath
    }
}
