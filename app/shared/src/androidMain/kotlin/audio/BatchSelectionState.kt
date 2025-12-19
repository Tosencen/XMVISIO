package com.xmvisio.app.audio

/**
 * 批量选择状态
 * 
 * 管理音频批量选择模式的状态和操作
 */
data class BatchSelectionState(
    val isActive: Boolean = false,
    val selectedAudioIds: Set<Long> = emptySet()
) {
    /**
     * 是否有选中项
     */
    val hasSelection: Boolean 
        get() = selectedAudioIds.isNotEmpty()
    
    /**
     * 选中数量
     */
    val selectionCount: Int 
        get() = selectedAudioIds.size
    
    /**
     * 切换音频的选中状态
     * 
     * @param audioId 音频ID
     * @return 更新后的状态
     */
    fun toggleSelection(audioId: Long): BatchSelectionState {
        return if (audioId in selectedAudioIds) {
            copy(selectedAudioIds = selectedAudioIds - audioId)
        } else {
            copy(selectedAudioIds = selectedAudioIds + audioId)
        }
    }
    
    /**
     * 全选所有音频
     * 
     * @param audioIds 音频ID列表
     * @return 更新后的状态
     */
    fun selectAll(audioIds: List<Long>): BatchSelectionState {
        return copy(selectedAudioIds = audioIds.toSet())
    }
    
    /**
     * 清除所有选中
     * 
     * @return 更新后的状态
     */
    fun clearSelection(): BatchSelectionState {
        return copy(selectedAudioIds = emptySet())
    }
    
    /**
     * 退出批量选择模式
     * 
     * @return 重置后的状态
     */
    fun exitMode(): BatchSelectionState {
        return BatchSelectionState()
    }
    
    /**
     * 检查音频是否被选中
     * 
     * @param audioId 音频ID
     * @return 是否选中
     */
    fun isSelected(audioId: Long): Boolean {
        return audioId in selectedAudioIds
    }
}
