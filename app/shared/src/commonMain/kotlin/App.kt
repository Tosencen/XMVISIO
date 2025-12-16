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
 * XMVISIO 应用入口
 */
@Composable
fun App() {
    val themeSettingsManager = remember { com.xmvisio.app.data.createThemeSettingsManager() }
    val themeSettings by themeSettingsManager.themeSettings.collectAsState(
        initial = com.xmvisio.app.data.ThemeSettings.Default
    )
    val coroutineScope = rememberCoroutineScope()
    
    AppTheme(themeSettings = themeSettings) {
        // 配置系统栏颜色
        com.xmvisio.app.ui.ConfigureSystemBars(
            isDark = when (themeSettings.darkMode) {
                com.xmvisio.app.data.DarkMode.LIGHT -> false
                com.xmvisio.app.data.DarkMode.DARK -> true
                com.xmvisio.app.data.DarkMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
            },
            statusBarColor = androidx.compose.ui.graphics.Color.Transparent,
            navigationBarColor = MaterialTheme.colorScheme.surfaceContainer
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
                    onNavigateToSettings = { navController.navigate("settings") }
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
                    themeSettings = themeSettings,
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
    modifier: Modifier = Modifier
) {
    // 使用 rememberSaveable 保存选中的 tab，避免从设置页返回时状态丢失
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.AUDIOBOOK) }
    
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var audioToPlay by remember { mutableStateOf<Any?>(null) }
    
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
        
        // 播放器底部弹出层（带动画效果）
        if (showPlayer && audioToPlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showPlayer = false
                        audioToPlay = null
                    }
            )
            
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

@OptIn(ExperimentalMaterial3Api::class)

@Composable
expect fun PlatformInfo()
