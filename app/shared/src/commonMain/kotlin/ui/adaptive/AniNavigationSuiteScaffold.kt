package com.xmvisio.app.ui.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自定义 NavigationSuiteScaffold，复刻 Animeko 的样式
 * - 导航栏背景：surfaceContainer
 * - 内容背景：surfaceContainerLowest
 * - 支持 navigationRailHeader（搜索按钮，带 48dp 间距）
 * - 支持 navigationRailFooter（设置按钮，仅在桌面端 NavigationRail 显示）
 * - 按钮之间 8dp 间距
 * 注意：在移动端（NavigationBar），设置按钮应该作为导航项添加到 navigationSuiteItems 中
 */
@Composable
fun AniNavigationSuiteScaffold(
    navigationSuiteItems: AniNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    navigationContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    navigationRailHeader: @Composable (ColumnScope.() -> Unit)? = null,
    navigationRailFooter: @Composable (ColumnScope.() -> Unit)? = null,
    navigationRailItemSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    // 判断当前布局类型
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo()
    )
    
    AniNavigationSuiteScaffoldContent(
        layoutType = layoutType,
        navigationSuiteItems = navigationSuiteItems,
        containerColor = containerColor,
        navigationContainerColor = navigationContainerColor,
        navigationRailHeader = navigationRailHeader,
        navigationRailFooter = navigationRailFooter,
        navigationRailItemSpacing = navigationRailItemSpacing,
        modifier = modifier,
        content = content
    )
}

@Composable
private fun AniNavigationSuiteScaffoldContent(
    layoutType: NavigationSuiteType,
    navigationSuiteItems: AniNavigationSuiteScope.() -> Unit,
    containerColor: Color,
    navigationContainerColor: Color,
    navigationRailHeader: @Composable (ColumnScope.() -> Unit)?,
    navigationRailFooter: @Composable (ColumnScope.() -> Unit)?,
    navigationRailItemSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (layoutType) {
        NavigationSuiteType.NavigationRail -> {
            // 宽屏：使用侧边栏布局
            Surface(
                modifier = modifier.fillMaxSize(),
                color = navigationContainerColor
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AniNavigationSuite(
                        layoutType = layoutType,
                        colors = NavigationSuiteDefaults.colors(
                            navigationBarContainerColor = navigationContainerColor,
                            navigationRailContainerColor = navigationContainerColor,
                            navigationDrawerContainerColor = navigationContainerColor
                        ),
                        navigationRailHeader = navigationRailHeader,
                        navigationRailFooter = navigationRailFooter,
                        navigationRailItemSpacing = navigationRailItemSpacing,
                        content = navigationSuiteItems
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = containerColor
                    ) {
                        content()
                    }
                }
            }
        }
        
        NavigationSuiteType.NavigationBar -> {
            // 窄屏（手机）：使用底部导航栏布局
            Surface(
                modifier = modifier.fillMaxSize(),
                color = containerColor
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        content()
                    }
                    
                    AniNavigationSuite(
                        layoutType = layoutType,
                        colors = NavigationSuiteDefaults.colors(
                            navigationBarContainerColor = navigationContainerColor,
                            navigationRailContainerColor = navigationContainerColor,
                            navigationDrawerContainerColor = navigationContainerColor
                        ),
                        navigationRailHeader = navigationRailHeader,
                        navigationRailFooter = navigationRailFooter,
                        navigationRailItemSpacing = navigationRailItemSpacing,
                        content = navigationSuiteItems
                    )
                }
            }
        }
        
        else -> {
            // NavigationDrawer 等其他类型
            Surface(
                modifier = modifier.fillMaxSize(),
                color = navigationContainerColor
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AniNavigationSuite(
                        layoutType = layoutType,
                        colors = NavigationSuiteDefaults.colors(
                            navigationBarContainerColor = navigationContainerColor,
                            navigationRailContainerColor = navigationContainerColor,
                            navigationDrawerContainerColor = navigationContainerColor
                        ),
                        content = navigationSuiteItems
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = containerColor
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
