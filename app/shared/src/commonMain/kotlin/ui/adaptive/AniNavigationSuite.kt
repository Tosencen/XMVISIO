package com.xmvisio.app.ui.adaptive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自定义 NavigationSuite，复刻 Animeko 的功能
 * - 支持 navigationRailHeader（搜索按钮）
 * - 支持 navigationRailFooter（设置按钮）
 * - 支持 navigationRailItemSpacing（按钮间距）
 */
@Composable
fun AniNavigationSuite(
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo()
    ),
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    navigationRailHeader: @Composable (ColumnScope.() -> Unit)? = null,
    navigationRailFooter: @Composable (ColumnScope.() -> Unit)? = null,
    navigationRailItemSpacing: Dp = 8.dp,
    content: AniNavigationSuiteScope.() -> Unit
) {
    // 使用 remember 确保 scope 在重组时保持稳定
    val scope = remember { AniNavigationSuiteScopeImpl() }
    // 每次重组时清空并重新填充
    scope.items.clear()
    scope.content()
    
    // 获取标题栏 insets（macOS 红绿黄按钮）
    val titleBarInsets = com.xmvisio.app.ui.LocalTitleBarInsets.current
    
    when (layoutType) {
        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = colors.navigationRailContainerColor,
                contentColor = colors.navigationRailContentColor,
                header = if (navigationRailHeader != null) {
                    {
                        // 为 header 添加标题栏间距
                        Column(
                            modifier = Modifier.windowInsetsPadding(titleBarInsets)
                        ) {
                            navigationRailHeader()
                        }
                    }
                } else null
            ) {
                // 为第一个导航项添加顶部间距（避免与 macOS 红绿黄按钮重叠）
                Spacer(Modifier.windowInsetsPadding(titleBarInsets))
                
                // 导航项
                scope.items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = item.icon,
                        modifier = item.modifier
                            .then(if (index < scope.items.size - 1) Modifier.padding(bottom = navigationRailItemSpacing) else Modifier),
                        enabled = item.enabled,
                        label = item.label,
                        alwaysShowLabel = item.alwaysShowLabel
                    )
                }
                
                // 底部弹性空间
                Spacer(Modifier.weight(1f))
                
                // Footer（设置按钮等）
                navigationRailFooter?.invoke(this)
            }
        }
        
        NavigationSuiteType.NavigationBar -> {
            NavigationBar(
                modifier = modifier,
                containerColor = colors.navigationBarContainerColor,
                contentColor = colors.navigationBarContentColor
            ) {
                scope.items.forEach { item ->
                    NavigationBarItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = item.icon,
                        enabled = item.enabled,
                        label = item.label,
                        alwaysShowLabel = item.alwaysShowLabel
                    )
                }
                // 注意：在移动端（NavigationBar），设置按钮应该作为导航项添加到 navigationSuiteItems 中
                // navigationRailFooter 仅在桌面端（NavigationRail）使用
            }
        }
        
        else -> {
            // NavigationDrawer 等其他类型暂不实现
            NavigationRail(modifier = modifier) {
                scope.items.forEach { item ->
                    NavigationRailItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = item.icon
                    )
                }
            }
        }
    }
}

interface AniNavigationSuiteScope {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null,
        alwaysShowLabel: Boolean = true,
    )
}

private class AniNavigationSuiteScopeImpl : AniNavigationSuiteScope {
    val items = mutableListOf<NavigationItem>()
    
    override fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        label: @Composable (() -> Unit)?,
        alwaysShowLabel: Boolean,
    ) {
        items.add(
            NavigationItem(
                selected = selected,
                onClick = onClick,
                icon = icon,
                modifier = modifier,
                enabled = enabled,
                label = label,
                alwaysShowLabel = alwaysShowLabel
            )
        )
    }
}

private data class NavigationItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val modifier: Modifier = Modifier,
    val enabled: Boolean = true,
    val label: @Composable (() -> Unit)? = null,
    val alwaysShowLabel: Boolean = true,
)
