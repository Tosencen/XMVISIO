package com.xmvisio.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 音频分类
 */
@Serializable
data class AudioCategory(
    val id: String,
    val name: String
) {
    companion object {
        val ALL = AudioCategory("all", "全部")
    }
}

/**
 * 音频分类映射（音频ID -> 单个分类ID）
 */
@Serializable
data class AudioCategoryMapping(
    val audioId: Long,
    val categoryId: String?  // null 表示未分类
)

/**
 * 分类管理器
 */
class CategoryManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("audio_categories", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_MAPPINGS = "mappings"
    }
    
    /**
     * 获取所有分类
     */
    suspend fun getCategories(): List<AudioCategory> = withContext(Dispatchers.IO) {
        val categoriesJson = prefs.getString(KEY_CATEGORIES, null) ?: return@withContext listOf(AudioCategory.ALL)
        try {
            val categories = json.decodeFromString<List<AudioCategory>>(categoriesJson)
            listOf(AudioCategory.ALL) + categories
        } catch (e: Exception) {
            listOf(AudioCategory.ALL)
        }
    }
    
    /**
     * 添加分类
     */
    suspend fun addCategory(name: String): AudioCategory = withContext(Dispatchers.IO) {
        val categories = getCategories().filter { it.id != AudioCategory.ALL.id }.toMutableList()
        val newCategory = AudioCategory(
            id = "category_${System.currentTimeMillis()}",
            name = name
        )
        categories.add(newCategory)
        saveCategories(categories)
        newCategory
    }
    
    /**
     * 删除分类
     */
    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val categories = getCategories().filter { it.id != AudioCategory.ALL.id && it.id != categoryId }
        saveCategories(categories)
        
        // 同时清除该分类的所有映射
        val mappings = getMappings().map { mapping ->
            if (mapping.categoryId == categoryId) {
                mapping.copy(categoryId = null)
            } else {
                mapping
            }
        }
        saveMappings(mappings)
    }
    
    /**
     * 重命名分类（保留映射关系）
     */
    suspend fun renameCategory(categoryId: String, newName: String) = withContext(Dispatchers.IO) {
        val categories = getCategories().filter { it.id != AudioCategory.ALL.id }.toMutableList()
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) {
            categories[index] = categories[index].copy(name = newName)
            saveCategories(categories)
        }
    }
    
    /**
     * 获取音频的分类ID
     */
    suspend fun getAudioCategory(audioId: Long): String? = withContext(Dispatchers.IO) {
        val mappings = getMappings()
        mappings.find { it.audioId == audioId }?.categoryId
    }
    
    /**
     * 设置音频的分类（单选）
     */
    suspend fun setAudioCategory(audioId: Long, categoryId: String?) = withContext(Dispatchers.IO) {
        val mappings = getMappings().toMutableList()
        val existingIndex = mappings.indexOfFirst { it.audioId == audioId }
        
        if (existingIndex >= 0) {
            mappings[existingIndex] = AudioCategoryMapping(audioId, categoryId)
        } else {
            mappings.add(AudioCategoryMapping(audioId, categoryId))
        }
        
        saveMappings(mappings)
    }
    
    /**
     * 获取分类下的所有音频ID
     */
    suspend fun getAudioIdsByCategory(categoryId: String): List<Long> = withContext(Dispatchers.IO) {
        if (categoryId == AudioCategory.ALL.id) {
            // "全部"返回空列表，表示不过滤（显示所有音频）
            return@withContext emptyList()
        }
        
        val mappings = getMappings()
        mappings.filter { it.categoryId == categoryId }.map { it.audioId }
    }
    
    private fun saveCategories(categories: List<AudioCategory>) {
        val json = json.encodeToString(categories)
        prefs.edit().putString(KEY_CATEGORIES, json).apply()
    }
    
    private fun getMappings(): List<AudioCategoryMapping> {
        val mappingsJson = prefs.getString(KEY_MAPPINGS, null) ?: return emptyList()
        return try {
            json.decodeFromString(mappingsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveMappings(mappings: List<AudioCategoryMapping>) {
        val json = json.encodeToString(mappings)
        prefs.edit().putString(KEY_MAPPINGS, json).apply()
    }
}
