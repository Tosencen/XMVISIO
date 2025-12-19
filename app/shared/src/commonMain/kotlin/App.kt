package com.xmvisio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
import com.xmvisio.app.ui.main.AudiobookScreen
import com.xmvisio.app.ui.main.DownloadsScreen
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
fun App(openPlayerAudioId: Long? = null) {
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
                        openPlayerAudioId = openPlayerAudioId
                    )
                }
                composable("settings") {
                    com.xmvisio.app.ui.settings.SettingsScreen(
                        onNavigateToTheme = { navController.navigate("theme_settings") },
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
    openPlayerAudioId: Long? = null
) {
    // 使用 rememberSaveable 保存选中的 tab，避免从设置页返回时状态丢失
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.AUDIOBOOK) }
    
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var audioToPlay by remember { mutableStateOf<Any?>(null) }
    
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
                MainTab.AUDIOBOOK -> AudiobookScreen(
                    onNavigateToPlayer = { audio ->
                        audioToPlay = audio
                        showPlayer = true
                    }
                )
                MainTab.DOWNLOADS -> DownloadsScreen(onNavigateToSettings = onNavigateToSettings)
            }
        }
        
        // 播放器全屏显示
        if (showPlayer && audioToPlay != null) {
            com.xmvisio.app.ui.player.AudioPlayerScreenWrapper(
                audio = audioToPlay!!,
                onClose = {
                    showPlayer = false
                    audioToPlay = null
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
    DOWNLOADS(
        label = "下载",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
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

@OptIn(ExperimentalMaterial3Api::class)

@Composable
expect fun PlatformInfo()
