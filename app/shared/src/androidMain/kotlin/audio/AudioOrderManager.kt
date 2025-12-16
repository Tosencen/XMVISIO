package com.xmvisio.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频排序管理器
 * 用于保存和加载用户自定义的音频排序
 */
class AudioOrderManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("audio_order", Context.MODE_PRIVATE)
    
    /**
     * 保存音频列表的自定义排序
     * @param categoryId 分类ID，null表示"全部"分类
     * @param audioIds 排序后的音频ID列表
     */
    suspend fun saveOrder(categoryId: String?, audioIds: List<Long>) = withContext(Dispatchers.IO) {
        val key = "order_${categoryId ?: "all"}"
        val value = audioIds.joinToString(",")
        prefs.edit().putString(key, value).apply()
    }
    
    /**
     * 获取音频列表的自定义排序
     * @param categoryId 分类ID，null表示"全部"分类
     * @return 排序后的音频ID列表，如果没有自定义排序则返回null
     */
    suspend fun getOrder(categoryId: String?): List<Long>? = withContext(Dispatchers.IO) {
        val key = "order_${categoryId ?: "all"}"
        val value = prefs.getString(key, null)
        value?.split(",")?.mapNotNull { it.toLongOrNull() }
    }
    
    /**
     * 清除指定分类的自定义排序
     */
    suspend fun clearOrder(categoryId: String?) = withContext(Dispatchers.IO) {
        val key = "order_${categoryId ?: "all"}"
        prefs.edit().remove(key).apply()
    }
    
    /**
     * 检查是否有自定义排序
     */
    suspend fun hasCustomOrder(categoryId: String?): Boolean = withContext(Dispatchers.IO) {
        val key = "order_${categoryId ?: "all"}"
        prefs.contains(key)
    }
}
