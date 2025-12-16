package com.xmvisio.app.media

import android.net.Uri
import androidx.activity.ComponentActivity

/**
 * 媒体服务接口
 * 处理音频文件的删除和重命名操作
 */
interface MediaService {
    /**
     * 初始化服务（注册 Activity Result Launcher）
     */
    fun initialize(activity: ComponentActivity)
    
    /**
     * 删除媒体文件
     * @param uris 要删除的文件 URI 列表
     * @return 是否成功
     */
    suspend fun deleteMedia(uris: List<Uri>): Boolean
    
    /**
     * 重命名媒体文件
     * @param uri 要重命名的文件 URI
     * @param newName 新文件名（包含扩展名）
     * @return 是否成功
     */
    suspend fun renameMedia(uri: Uri, newName: String): Boolean
}
