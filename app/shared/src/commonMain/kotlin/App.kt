package com.xmvisio.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
// import androidx.compose.material.icons.filled.Download  // 下载功能已禁用
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.AudioFile
// import androidx.compose.material.icons.outlined.Download  // 下载功能已禁用
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.*
import com.xmvisio.app.ui.adaptive.AniNavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xmvisio.app.data.VideoInfo
import com.xmvisio.app.ui.main.AudiobookScreen
//import com.xmvisio.app.ui.main.DownloadsScreen
import com.xmvisio.app.ui.main.VideoScreen
import com.xmvisio.app.ui.player.VideoPlayerScreen
import com.xmvisio.app.ui.theme.AppTheme

/**
 * 带更新检查的应用入口（Android 专用）
 */
@Composable
expect fun AppWithUpdateCheck(updateViewModel: Any, openPlayerAudioId: Long? = null)

/**
 * XMVISIO 应用入口
 */
@Composable
fun App(
    openPlayerAudioId: Long? = null,
    updateAvailable: Boolean = false,
    onUpdateCheck: () -> Unit = {}
) {
    val themeSettingsManager = remember { com.xmvisio.app.data.createThemeSettingsManager() }
    
    // 使用 produceState 确保在主题加载完成前不渲染 UI，避免颜色闪烁
    val themeSettings by produceState<com.xmvisio.app.data.ThemeSettings?>(initialValue = null) {
        themeSettingsManager.themeSettings.collect { settings ->
            value = settings
        }
    }
    val coroutineScope = rememberCoroutineScope()
    
    // 主题未加载完成时不渲染，避免从后台恢复时颜色闪烁
    val currentThemeSettings = themeSettings ?: return
    
    AppTheme(themeSettings = currentThemeSettings) {
        // 配置系统栏颜色（统一配置点）
        com.xmvisio.app.ui.ConfigureSystemBars(
            isDark = when (currentThemeSettings.darkMode) {
                com.xmvisio.app.data.DarkMode.LIGHT -> false
                com.xmvisio.app.data.DarkMode.DARK -> true
                com.xmvisio.app.data.DarkMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
            },
            statusBarColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            navigationBarColor = androidx.compose.ui.graphics.Color.Transparent
        )
        
        val navController = rememberNavController()
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    MainScreen(
                        onNavigateToSettings = { navController.navigate("settings") },
                        openPlayerAudioId = openPlayerAudioId,
                        updateAvailable = updateAvailable,
                        onUpdateCheck = onUpdateCheck
                    )
                }
                composable("settings") {
                    com.xmvisio.app.ui.settings.SettingsScreen(
                        onNavigateToTheme = { navController.navigate("theme_settings") },
                        onNavigateToVideoPlayerSettings = { navController.navigate("video_player_settings") },
                        onNavigateToFolderPreferences = { navController.navigate("folder_preferences") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("theme_settings") {
                    com.xmvisio.app.ui.settings.ThemeSettingsPage(
                        themeSettings = currentThemeSettings,
                        onThemeChange = { newSettings ->
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                themeSettingsManager.saveThemeSettings(newSettings)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("video_player_settings") {
                    com.xmvisio.app.ui.settings.VideoPlayerPreferencesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("folder_preferences") {
                    com.xmvisio.app.ui.settings.FolderPreferencesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * 主界面 - 自适应导航布局
 * Desktop: NavigationRail (左侧)
 * Mobile: NavigationBar (底部)
 */
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    openPlayerAudioId: Long? = null,
    updateAvailable: Boolean = false,
    onUpdateCheck: () -> Unit = {}
) {
    // 使用 rememberSaveable 保存选中的 tab，避免从设置页返回时状态丢失
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.AUDIOBOOK) }

    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var audioToPlay by remember { mutableStateOf<Any?>(null) }

    var showVideoPlayer by rememberSaveable { mutableStateOf(false) }
    var videoToPlay by remember { mutableStateOf<VideoInfo?>(null) }
    var videoList by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }
    var videoIndex by remember { mutableIntStateOf(0) }

    // 如果有 openPlayerAudioId，从通知点击进来，直接打开播放器
    // 这个功能只在 Android 上可用
    if (openPlayerAudioId != null && openPlayerAudioId > 0) {
        // 使用 expect/actual 来处理平台特定的逻辑
        HandleOpenPlayerRequest(
            audioId = openPlayerAudioId,
            onAudioFound = { audio ->
                audioToPlay = audio
                showPlayer = true
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AniNavigationSuiteScaffold(
            navigationSuiteItems = {
            MainTab.entries.forEach { tab ->
                item(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    alwaysShowLabel = true
                )
            }
            },
            navigationRailHeader = null,
            navigationRailFooter = null,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            navigationContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            when (selectedTab) {
                MainTab.AUDIOBOOK -> {
                    AudiobookScreen(
                        onNavigateToPlayer = { audio ->
                            audioToPlay = audio
                            showPlayer = true
                        },
                        updateAvailable = updateAvailable,
                        onUpdateCheck = onUpdateCheck
                    )
                }
                MainTab.VIDEO -> {
                    VideoScreen(
                        onNavigateToPlayer = { video, videos ->
                            videoToPlay = video
                            videoList = videos
                            videoIndex = videos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                            showVideoPlayer = true
                        },
                        onNavigateToSettings = onNavigateToSettings,
                        updateAvailable = updateAvailable,
                        onUpdateCheck = onUpdateCheck
                    )
                }
            }
        }

        // 音频播放器全屏显示
        if (showPlayer && audioToPlay != null) {
            com.xmvisio.app.ui.player.AudioPlayerScreenWrapper(
                audio = audioToPlay!!,
                onClose = {
                    showPlayer = false
                    audioToPlay = null
                }
            )
        }

        // 视频播放器全屏显示
        if (showVideoPlayer && videoToPlay != null) {
            VideoPlayerScreen(
                video = videoToPlay!!,
                videos = videoList,
                currentIndex = videoIndex,
                onClose = {
                    showVideoPlayer = false
                    videoToPlay = null
                },
                onNavigateToVideo = { index ->
                    videoIndex = index
                    videoToPlay = videoList.getOrNull(index)
                }
            )
        }
    }
}

/**
 * 主导航页面
 */
enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    AUDIOBOOK(
        label = "有声",
        selectedIcon = Icons.Filled.AudioFile,
        unselectedIcon = Icons.Outlined.AudioFile
    ),
    /*DOWNLOADS(
        label = "下载",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    ),*/
    VIDEO(
        label = "视频",
        selectedIcon = Icons.Filled.VideoFile,
        unselectedIcon = Icons.Outlined.VideoFile
    )
}

/**
 * 处理从通知打开播放器的请求（平台特定）
 */
@Composable
expect fun HandleOpenPlayerRequest(
    audioId: Long,
    onAudioFound: (Any) -> Unit
)

@Composable
expect fun PlatformInfo()
