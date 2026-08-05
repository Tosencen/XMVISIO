package com.xmvisio.app.ui.main

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmvisio.app.audio.AudioCategory
import com.xmvisio.app.audio.AudioManager
import com.xmvisio.app.audio.AudioOperationResult
import com.xmvisio.app.audio.AudioOrderManager
import com.xmvisio.app.audio.AudioScanner
import com.xmvisio.app.audio.BatchSelectionState
import com.xmvisio.app.audio.CategoryManager
import com.xmvisio.app.audio.GlobalAudioManager
import com.xmvisio.app.audio.GlobalAudioPlayer
import com.xmvisio.app.audio.GlobalAudioPlayerController
import com.xmvisio.app.audio.LocalAudioFile
import com.xmvisio.app.audio.RecentPlayManager
import com.xmvisio.app.permissions.AudioPermissionManager
import com.xmvisio.app.permissions.PermissionStatus
import com.xmvisio.app.ui.components.ToastViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * AudiobookScreen 的状态持有者（ViewModel）
 * 统一管理所有 UI 状态和业务逻辑。
 *
 * 作为 ViewModel 与 Activity/组合解耦：从设置页返回、切换 Tab 或
 * 旋转屏幕（进程未销毁）时状态保留，避免重复全量扫描媒体库。
 */
class AudiobookScreenState(context: Context) : ViewModel() {
    /** 只持有 ApplicationContext，避免持有 Activity 造成内存泄漏 */
    val context: Context = context.applicationContext
    /**
     * Intent 启动器回调（由 composable 在创建后绑定）
     */
    var onLaunchIntent: (IntentSenderRequest) -> Unit = {}
    // ===== 权限管理 =====
    val permissionManager = AudioPermissionManager(context)
    var permissionStatus by mutableStateOf(permissionManager.checkPermissionStatus())

    // ===== 音频扫描和管理 =====
    val audioScanner = AudioScanner(context)
    val audioManager = GlobalAudioManager.getInstance(context)
    val categoryManager = CategoryManager(context)
    val audioOrderManager = AudioOrderManager(context)

    // ===== 音频数据 =====
    var audioList by mutableStateOf<List<LocalAudioFile>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)

    // ===== 分类状态 =====
    var categories by mutableStateOf<List<AudioCategory>>(listOf(AudioCategory.ALL))
    var selectedCategory by mutableStateOf(AudioCategory.ALL)
    var currentCategory by mutableStateOf<AudioCategory?>(null)
    val isCategoryView: Boolean get() = currentCategory != null

    // ===== 搜索状态 =====
    var isSearching by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    // ===== 排序模式 =====
    var isReorderMode by mutableStateOf(false)

    // ===== 批量选择 =====
    var batchSelectionState by mutableStateOf(BatchSelectionState())

    // ===== 对话框/菜单可见性 =====
    var showContextMenu by mutableStateOf(false)
    var showRenameDialog by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)
    var showCategoryDialog by mutableStateOf(false)
    var showPropertiesDialog by mutableStateOf(false)
    var showAddCategoryDialog by mutableStateOf(false)
    var isAddCategoryFromSelection by mutableStateOf(false)
    var showDeleteCategoryDialog by mutableStateOf(false)
    var showRenameCategoryDialog by mutableStateOf(false)
    var showCategoryManageDialog by mutableStateOf(false)
    var showBatchDeleteDialog by mutableStateOf(false)
    var showSettingsMenu by mutableStateOf(false)
    var showSeekTimeDialog by mutableStateOf(false)
    var showAutoRewindDialog by mutableStateOf(false)

    // ===== 选中项 =====
    var selectedAudio by mutableStateOf<LocalAudioFile?>(null)
    var categoryToDelete by mutableStateOf<AudioCategory?>(null)
    var categoryToEdit by mutableStateOf<AudioCategory?>(null)
    var newCategoryName by mutableStateOf("")
    var renameText by mutableStateOf("")

    // ===== 分类缓存 =====
    val categorySortedListsCache = mutableStateMapOf<String, List<LocalAudioFile>>()
    var categoryMappingVersion by mutableIntStateOf(0)
    var currentPageAudioList by mutableStateOf<List<LocalAudioFile>>(emptyList())

    // ===== 文件夹计数缓存（ViewModel 持有，避免每次进入文件夹主页都查 DB）=====
    // 版本键逻辑见 CacheLogic.kt 的 VersionedCache：仅版本变化时才重查 DB。
    var categoryCounts by mutableStateOf<Map<String, Int>>(emptyMap())
    private val categoryCountsCache = VersionedCache<Map<String, Int>> {
        val counts = mutableMapOf<String, Int>()
        categories.filter { it.id != AudioCategory.ALL.id }.forEach { category ->
            counts[category.id] = categoryManager.getAudioIdsByCategory(category.id).size
        }
        counts
    }

    /**
     * 加载/刷新各分类的音频计数。仅在分类或映射变化（version 变化）时重新查询 DB。
     */
    fun ensureCategoryCountsLoaded() {
        if (categoryCountsCache.needLoad(categoryMappingVersion)) {
            viewModelScope.launch {
                categoryCounts = categoryCountsCache.get(categoryMappingVersion)
            }
        }
    }

    /** 分类增删改后强制刷新文件夹计数（分类列表变了但 mappingVersion 未变） */
    private fun invalidateCategoryCounts() {
        categoryCountsCache.invalidate()
    }

    // ===== 播放设置 =====
    private val prefs = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
    var seekTimeInSeconds by mutableIntStateOf(prefs.getInt("seek_time", 10))
    var autoRewindInSeconds by mutableIntStateOf(prefs.getInt("auto_rewind", 2))

    // ===== 最近播放 =====
    val recentPlayManager = RecentPlayManager.getInstance(context)

    // ===== 播放位置缓存（一次批量读取，避免每个列表项各自异步读 prefs）=====
    // 刷新时机语义见 CacheLogic.kt 的 AudioPositionsCache：每次 refresh 都无条件重载，
    // 保证播放过音频后返回列表能看到最新进度。
    private val positionsCache = AudioPositionsCache()
    var audioPositions by mutableStateOf<Map<Long, kotlin.time.Duration>>(emptyMap())
        private set

    /**
     * 加载所有音频的播放位置。每次扫描后调用（无条件重载，无一次性守卫）。
     */
    fun loadAudioPositions() {
        viewModelScope.launch {
            positionsCache.refresh { positionManager.getAllPositions() }
            audioPositions = positionsCache.positions
        }
    }

    fun getSavedPosition(audioId: Long): kotlin.time.Duration {
        return positionsCache.get(audioId)
    }

    private val positionManager = com.xmvisio.app.audio.PlaybackPositionManager(context)

    // ===== Toast =====
    val toastViewModel = ToastViewModel()

    // ===== 全局播放器引用 =====
    val globalPlayer = GlobalAudioPlayer.getInstance(context)
    val globalController = GlobalAudioPlayerController.getInstance(context)

    // ===== 计算属性（逻辑见 MediaListLogic.kt，纯函数可单测）=====
    val filteredAudioList: List<LocalAudioFile> by derivedStateOf {
        if (searchQuery.isNotBlank()) {
            audioList.filter { matchesMediaQuery(it.title, it.artist, searchQuery) }
        } else {
            audioList
        }
    }

    // ===== 扫描音频文件 =====
    fun scanAudioFiles(isRefresh: Boolean) {
        if (isRefresh) {
            isRefreshing = true
        } else {
            isLoading = true
        }
        viewModelScope.launch {
            if (isRefresh) delay(300)
            audioList = audioScanner.scanAudioFiles()
            categories = categoryManager.getCategories()
            isRefreshing = false
            isLoading = false
            // 扫描完成后加载一次播放位置（只读一次 prefs，避免列表项各自异步读）
            loadAudioPositions()
        }
    }

    // ===== 获取权限请求权限名 =====
    fun getRequiredPermission(): String = AudioPermissionManager.getRequiredPermission()

    // ===== 打开应用设置 =====
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            // applicationContext 启动 Activity 必须带 NEW_TASK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ===== 请求权限更新 =====
    fun onPermissionResult(granted: Boolean) {
        permissionStatus = if (granted) {
            PermissionStatus.GRANTED
        } else {
            permissionManager.checkPermissionStatus()
        }
        if (granted) {
            scanAudioFiles(false)
        }
    }

    // ===== 生命周期回调 =====
    fun createLifecycleObserver(): LifecycleEventObserver {
        return LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = permissionManager.checkPermissionStatus()
                if (permissionStatus == PermissionStatus.GRANTED && audioList.isEmpty()) {
                    scanAudioFiles(false)
                }
            }
        }
    }

    // ===== 媒体库变更观察（新文件自动刷新，替代旧版“每次返回都全量扫描”）=====
    var lastObserverChange by mutableLongStateOf(0L)

    fun createAudioContentObserver(): ContentObserver {
        return object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()
                if (now - lastObserverChange > 300) {
                    lastObserverChange = now
                    // 不显示全屏 loading，只触发轻量刷新
                    scanAudioFiles(true)
                }
            }
        }
    }

    // ===== 返回键处理 =====
    fun onBackInSearch(): Boolean {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
            return true
        }
        return false
    }

    fun onBackInReorder(): Boolean {
        if (isReorderMode) {
            isReorderMode = false
            return true
        }
        return false
    }

    fun onBackInBatchSelection(): Boolean {
        if (batchSelectionState.isActive) {
            batchSelectionState = batchSelectionState.exitMode()
            return true
        }
        return false
    }

    fun onBackInCategoryView(): Boolean {
        if (isCategoryView) {
            currentCategory = null
            selectedCategory = AudioCategory.ALL
            if (batchSelectionState.isActive) {
                batchSelectionState = batchSelectionState.clearSelection()
            }
            return true
        }
        return false
    }

    // ===== 音频操作 =====
    fun renameAudio(uri: Uri, newName: String) {
        viewModelScope.launch {
            when (val result = audioManager.renameAudio(uri, newName)) {
                is AudioOperationResult.Success -> {
                    delay(1000)
                    scanAudioFiles(false)
                }
                is AudioOperationResult.NeedPermission -> {
                    onLaunchIntent(IntentSenderRequest.Builder(result.intentSender).build())
                }
                is AudioOperationResult.Failed -> { /* 静默失败 */ }
            }
        }
        showRenameDialog = false
    }

    /**
     * 删除成功后清理分类映射孤儿数据 + 让相关分类的排序缓存失效。
     * 注意：清理是“尽力而为”，失败不阻断主流程（列表更新）。
     */
    private suspend fun cleanupAfterDelete(audioIds: List<Long>) {
        if (audioIds.isEmpty()) return
        try {
            categoryManager.deleteAudioMappings(audioIds)
        } catch (e: Exception) {
            android.util.Log.w("AudiobookScreenState", "清理分类映射失败", e)
        }
        invalidateCategoryCachesContaining(audioIds.toSet())
        categoryMappingVersion++
    }

    private fun invalidateCategoryCachesContaining(audioIds: Set<Long>) {
        if (audioIds.isEmpty()) return
        categorySortedListsCache.entries.toList()
            .filter { (_, list) -> list.any { it.id in audioIds } }
            .forEach { (key, _) -> categorySortedListsCache.remove(key) }
    }

    fun deleteAudio(uri: Uri) {
        val audioId = selectedAudio?.id
        viewModelScope.launch {
            when (val result = audioManager.deleteAudio(uri)) {
                is AudioOperationResult.Success -> {
                    cleanupAfterDelete(listOfNotNull(audioId))
                    audioList = audioList.filter { it.id != audioId }
                }
                is AudioOperationResult.NeedPermission -> {
                    onLaunchIntent(IntentSenderRequest.Builder(result.intentSender).build())
                }
                is AudioOperationResult.Failed -> { /* 静默失败 */ }
            }
        }
        showDeleteDialog = false
    }

    fun setAudioCategory(audioId: Long, categoryId: String?) {
        viewModelScope.launch {
            categoryManager.setAudioCategory(audioId, categoryId)
            categories = categoryManager.getCategories()
            categoryMappingVersion++
        }
        showCategoryDialog = false
    }

    // ===== 分类操作 =====
    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryManager.addCategory(name.trim())
            categories = categoryManager.getCategories()
            invalidateCategoryCounts()
            newCategoryName = ""
            showAddCategoryDialog = false
            if (isAddCategoryFromSelection) {
                showCategoryDialog = true
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryManager.deleteCategory(categoryId)
            categories = categoryManager.getCategories()
            if (selectedCategory.id == categoryId) {
                selectedCategory = AudioCategory.ALL
            }
            // 如果删除的是当前正在查看的分类文件夹，退回首页
            if (currentCategory?.id == categoryId) {
                currentCategory = null
                selectedCategory = AudioCategory.ALL
            }
            categorySortedListsCache.remove(categoryId)
            invalidateCategoryCounts()
            categoryToDelete = null
            showDeleteCategoryDialog = false
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            categoryManager.renameCategory(categoryId, newName)
            categories = categoryManager.getCategories()
            renameText = ""
            showRenameCategoryDialog = false
            categoryToEdit = null
        }
    }

    // ===== 批量操作 =====
    fun batchSetCategory(category: AudioCategory) {
        viewModelScope.launch {
            val selectedIds = batchSelectionState.selectedAudioIds.toList()
            val count = batchSelectionState.selectionCount
            val categoryName = category.name
            batchSelectionState = batchSelectionState.exitMode()

            val result = categoryManager.setAudioCategoryBatch(
                audioIds = selectedIds,
                categoryId = category.id
            )
            categoryMappingVersion++
            if (result.isSuccess) {
                toastViewModel.show("已将 $count 个音频添加到「$categoryName」", viewModelScope)
            } else {
                toastViewModel.show("操作失败，请重试", viewModelScope)
            }
        }
    }

    fun batchDelete() {
        viewModelScope.launch {
            val selectedIds = batchSelectionState.selectedAudioIds.toList()
            val count = batchSelectionState.selectionCount
            val selectedAudios = audioList.filter { it.id in selectedIds }
            val uris = selectedAudios.map { it.uri }

            showBatchDeleteDialog = false
            batchSelectionState = batchSelectionState.exitMode()

            when (val result = audioManager.deleteAudioBatch(uris)) {
                is AudioOperationResult.Success -> {
                    // 清理分类映射孤儿数据 + 让包含这些音频的分类排序缓存失效
                    cleanupAfterDelete(selectedIds)
                    audioList = audioList.filter { it.id !in selectedIds }
                    toastViewModel.show("已删除 $count 个音频", viewModelScope)
                }
                is AudioOperationResult.NeedPermission -> {
                    onLaunchIntent(IntentSenderRequest.Builder(result.intentSender).build())
                }
                is AudioOperationResult.Failed -> {
                    toastViewModel.show("删除失败，请重试", viewModelScope)
                }
            }
        }
    }

    // ===== 播放设置 =====
    fun saveSeekTime(seconds: Int) {
        seekTimeInSeconds = seconds
        prefs.edit().putInt("seek_time", seconds).apply()
        showSeekTimeDialog = false
    }

    fun saveAutoRewind(seconds: Int) {
        autoRewindInSeconds = seconds
        prefs.edit().putInt("auto_rewind", seconds).apply()
        showAutoRewindDialog = false
    }

    // ===== 排序操作 =====
    fun saveOrder(categoryId: String?, orderedIds: List<Long>) {
        viewModelScope.launch {
            audioOrderManager.saveOrder(categoryId, orderedIds)
        }
    }

    /**
     * 计算分类的排序列表：缓存 ID 集合与 baseList 一致时复用缓存顺序（用最新对象），
     * 否则按自定义排序重算。逻辑见 MediaListLogic.kt 的纯函数（可单测）。
     */
    private fun computeCategorySortedList(categoryId: String, baseList: List<LocalAudioFile>): List<LocalAudioFile> {
        val cached = categorySortedListsCache[categoryId]
        // 仅当缓存与当前列表的 ID 集合完全一致时复用，否则重算。
        // 否则（produceState 初次空列表、音频移入/移出/删除后）会返回过期的空/旧列表。
        if (cached != null && hasSameIdSet(cached, baseList) { it.id }) {
            return rebuildWithCachedOrder(cached, baseList) { it.id }
        }
        val customOrder = audioOrderManager.getOrderSync(
            if (categoryId == AudioCategory.ALL.id) null else categoryId
        )
        return applyCustomOrderById(baseList, customOrder) { it.id }
    }

    fun getCategorySortedList(categoryId: String, baseList: List<LocalAudioFile>): List<LocalAudioFile> {
        return computeCategorySortedList(categoryId, baseList).also {
            categorySortedListsCache[categoryId] = it
        }
    }

    fun updateCategorySortedList(categoryId: String, baseList: List<LocalAudioFile>) {
        categorySortedListsCache[categoryId] = computeCategorySortedList(categoryId, baseList)
    }

    fun reorderInCategory(categoryId: String, fromIndex: Int, toIndex: Int) {
        val currentList = categorySortedListsCache[categoryId] ?: return
        val newList = moveItemInList(currentList, fromIndex, toIndex)
        categorySortedListsCache[categoryId] = newList
        saveOrder(
            if (categoryId == AudioCategory.ALL.id) null else categoryId,
            newList.map { it.id }
        )
    }

    // ===== 播放 =====
    fun playAudio(audio: LocalAudioFile) {
        viewModelScope.launch {
            recentPlayManager.recordRecentPlay(audio.id, audio.title)
        }
    }

    // ===== 清理 =====
    fun reset() {
        newCategoryName = ""
        renameText = ""
    }
}