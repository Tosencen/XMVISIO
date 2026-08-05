package com.xmvisio.app.audio

import android.content.Context
import com.xmvisio.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频排序管理器（SQLite 存储，见 [AppDatabase]）
 * 用于保存和加载用户自定义的音频排序
 */
class AudioOrderManager(private val context: Context) {

    private val db get() = AppDatabase.getInstance(context)

    /**
     * 保存音频列表的自定义排序
     * @param categoryId 分类ID，null表示"全部"分类
     * @param audioIds 排序后的音频ID列表
     */
    suspend fun saveOrder(categoryId: String?, audioIds: List<Long>) = withContext(Dispatchers.IO) {
        val key = categoryId ?: "all"
        val value = audioIds.joinToString(",")
        db.withConnection { conn ->
            conn.prepare(
                "INSERT INTO audio_order (category_id, audio_ids) VALUES (?, ?) " +
                    "ON CONFLICT(category_id) DO UPDATE SET audio_ids = excluded.audio_ids"
            ).use { stmt ->
                stmt.bindText(1, key)
                stmt.bindText(2, value)
                stmt.step()
            }
        }
    }

    /**
     * 获取音频列表的自定义排序
     * @param categoryId 分类ID，null表示"全部"分类
     * @return 排序后的音频ID列表，如果没有自定义排序则返回null
     */
    suspend fun getOrder(categoryId: String?): List<Long>? = withContext(Dispatchers.IO) {
        getOrderSync(categoryId)
    }

    /**
     * 同步获取音频列表的自定义排序（用于初始化）
     * @param categoryId 分类ID，null表示"全部"分类
     * @return 排序后的音频ID列表，如果没有自定义排序则返回null
     */
    fun getOrderSync(categoryId: String?): List<Long>? {
        val key = categoryId ?: "all"
        return db.withConnection { conn ->
            conn.prepare("SELECT audio_ids FROM audio_order WHERE category_id = ?").use { stmt ->
                stmt.bindText(1, key)
                if (stmt.step()) {
                    stmt.getText(0).split(",").mapNotNull { it.toLongOrNull() }
                } else {
                    null
                }
            }
        }
    }

    /**
     * 清除指定分类的自定义排序
     */
    suspend fun clearOrder(categoryId: String?) = withContext(Dispatchers.IO) {
        val key = categoryId ?: "all"
        db.withConnection { conn ->
            conn.prepare("DELETE FROM audio_order WHERE category_id = ?").use { stmt ->
                stmt.bindText(1, key)
                stmt.step()
            }
        }
    }

    /**
     * 检查是否有自定义排序
     */
    suspend fun hasCustomOrder(categoryId: String?): Boolean = withContext(Dispatchers.IO) {
        val key = categoryId ?: "all"
        db.withConnection { conn ->
            conn.prepare("SELECT 1 FROM audio_order WHERE category_id = ? LIMIT 1").use { stmt ->
                stmt.bindText(1, key)
                stmt.step()
            }
        }
    }
}
