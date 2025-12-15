package com.xmvisio.app.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 更新状态管理 ViewModel
 */
class UpdateViewModel(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // GitHub Token（可选）
    private val githubToken: String? = null  // 如需配置，从 BuildConfig 读取
    
    private val updateChecker = UpdateChecker(githubToken = githubToken)
    private val fileDownloader = FileDownloader()
    private val updateInstaller = UpdateInstaller(context)
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    val downloadProgress = fileDownloader.progress
    val downloadState = fileDownloader.state
    
    private var _latestVersion: NewVersion? = null
    val latestVersion: NewVersion?
        get() = _latestVersion
    
    /**
     * 检查更新
     */
    fun checkUpdate(currentVersion: String) {
        scope.launch {
            _updateState.value = UpdateState.Checking
            Log.d("UpdateViewModel", "开始检查更新，当前版本: $currentVersion")
            
            try {
                val newVersion = withContext(Dispatchers.IO) {
                    updateChecker.checkLatestVersion(currentVersion)
                }
                
                if (newVersion != null) {
                    _latestVersion = newVersion
                    _updateState.value = UpdateState.HasUpdate(newVersion)
                    Log.d("UpdateViewModel", "发现新版本: ${newVersion.version}")
                } else {
                    _updateState.value = UpdateState.UpToDate
                    Log.d("UpdateViewModel", "当前已是最新版本")
                }
            } catch (e: IOException) {
                val errorMsg = e.message ?: "网络连接失败，请检查网络后重试"
                Log.e("UpdateViewModel", "检查更新失败: $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "检查更新失败"
                Log.e("UpdateViewModel", "检查更新异常: $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            }
        }
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload() {
        val version = _latestVersion ?: return
        
        scope.launch {
            _updateState.value = UpdateState.Downloading(0f)
            
            // 获取下载目录
            val downloadDir = File(context.getExternalFilesDir(null), "updates")
            downloadDir.mkdirs()
            
            val apkFile = File(downloadDir, "XMVISIO-${version.version}.apk")
            
            // 监听下载进度
            val progressJob = scope.launch {
                downloadProgress.collect { progress ->
                    _updateState.value = UpdateState.Downloading(progress)
                }
            }
            
            // 监听下载状态
            val stateJob = scope.launch {
                downloadState.collect { state ->
                    when (state) {
                        is DownloadState.Success -> {
                            _updateState.value = UpdateState.Downloaded(state.file)
                            progressJob.cancel()
                        }
                        is DownloadState.Failed -> {
                            _updateState.value = UpdateState.DownloadFailed(state.error)
                            progressJob.cancel()
                        }
                        else -> {}
                    }
                }
            }
            
            // 下载文件
            val downloadedFile = withContext(Dispatchers.IO) {
                fileDownloader.download(version.downloadUrl, apkFile, version.fallbackUrl)
            }
            
            progressJob.cancel()
            stateJob.cancel()
            
            if (downloadedFile == null && _updateState.value !is UpdateState.Downloaded) {
                _updateState.value = UpdateState.DownloadFailed("下载失败")
            }
        }
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload() {
        fileDownloader.cancel()
        _updateState.value = UpdateState.Idle
    }
    
    /**
     * 安装 APK
     */
    fun installApk(file: File) {
        scope.launch {
            if (!updateInstaller.hasInstallPermission()) {
                _updateState.value = UpdateState.InstallPermissionRequested
                updateInstaller.requestInstallPermission()
                return@launch
            }
            
            _updateState.value = UpdateState.Installing
            
            val success = withContext(Dispatchers.IO) {
                updateInstaller.installApk(file)
            }
            
            if (!success) {
                _updateState.value = UpdateState.InstallFailed("安装失败")
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _updateState.value = UpdateState.Idle
    }
}

/**
 * 更新状态
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class HasUpdate(val version: NewVersion) : UpdateState()
    data class CheckFailed(val error: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class DownloadFailed(val error: String) : UpdateState()
    object Installing : UpdateState()
    object InstallPermissionRequested : UpdateState()
    data class InstallFailed(val error: String) : UpdateState()
}
