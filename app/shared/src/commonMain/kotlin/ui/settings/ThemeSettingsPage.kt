package com.xmvisio.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.xmvisio.app.data.DarkMode
import com.xmvisio.app.data.ThemeSettings
import com.xmvisio.app.ui.theme.ThemeColorOptions
import com.xmvisio.app.ui.theme.appColorScheme
import com.xmvisio.app.ui.LocalTitleBarInsets

/**
 * 主题设置页面
 * 复刻 Animeko 的 UI 布局风格 + 滚动交互
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsPage(
    themeSettings: ThemeSettings,
    onThemeChange: (ThemeSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 滚动行为：null = 不使用滚动行为，TopAppBar 固定
    val scrollBehavior: TopAppBarScrollBehavior? = null
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    
    // 获取标题栏 insets（macOS 上为红绿黄按钮留出空间）
    val titleBarInsets = LocalTitleBarInsets.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("主题")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .union(titleBarInsets),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = containerColor,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主题模式组（无背景）
            SettingsGroupPlain(
                title = "主题模式"
            ) {
                DarkModeSelector(
                    themeSettings = themeSettings,
                    currentMode = themeSettings.darkMode,
                    onModeSelected = { mode ->
                        onThemeChange(themeSettings.copy(darkMode = mode))
                    }
                )
            }
            
            // 主题设置组（复刻 Animeko：透明背景，每个 item 独立卡片）
            SettingsGroupPlain(
                title = "主题设置"
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 动态颜色（样式与纯黑背景一致；置于其上方）
                    val supportDynamic = com.xmvisio.app.ui.theme.isPlatformSupportDynamicTheme()
                    SettingsSwitchItem(
                        title = "动态颜色",
                        description = if (supportDynamic) {
                            "使用动态配色方案（Android 12+ 从壁纸提取，其他平台基于种子颜色）"
                        } else {
                            "当前平台不支持动态颜色"
                        },
                        checked = themeSettings.useDynamicTheme,
                        onCheckedChange = { checked ->
                            onThemeChange(themeSettings.copy(useDynamicTheme = checked))
                        },
                        enabled = supportDynamic
                    )
                    
                    SettingsSwitchItem(
                        title = "纯黑背景",
                        description = "深色模式下使用纯黑背景",
                        checked = themeSettings.useBlackBackground,
                        onCheckedChange = { checked ->
                            onThemeChange(themeSettings.copy(useBlackBackground = checked))
                        }
                    )
                }
            }
            
            // 主题配色（无背景）
            Box(
                modifier = Modifier.alpha(if (themeSettings.useDynamicTheme) 0.5f else 1f)
            ) {
                SettingsGroupPlain(
                    title = "主题配色"
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorPaletteGrid(
                        colors = ThemeColorOptions,
                        selectedColor = themeSettings.seedColor,
                        enabled = !themeSettings.useDynamicTheme,
                        onColorSelect = { color ->
                            onThemeChange(themeSettings.copy(
                                seedColorValue = color.value,
                                useDynamicTheme = false
                            ))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 设置分组容器（无背景）
 * 用于深色模式、主题配色等
 */
@Composable
private fun SettingsGroupPlain(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp)
    ) {
        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // 内容
        content()
    }
}

/**
 * 设置开关项
 * 复刻 Animeko 的 SwitchItem 样式
 * 注意：使用 InteractionSource 防止 Switch 点击时触发 ListItem 的 clickable
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // 使用独立的 InteractionSource，防止事件冲突
    val switchInteractionSource = remember { MutableInteractionSource() }
    val listItemInteractionSource = remember { MutableInteractionSource() }
    
    // 使用状态防止重复触发，特别是在首次渲染时
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val handleChange = { newValue: Boolean ->
        if (!isProcessing && enabled) {
            isProcessing = true
            onCheckedChange(newValue)
            // 短暂延迟后重置，确保动画完成
            coroutineScope.launch {
                delay(200)
                isProcessing = false
            }
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = title,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            },
            supportingContent = description?.let { 
                { 
                    Text(
                        text = it,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                } 
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = handleChange,
                    enabled = enabled && !isProcessing,
                    interactionSource = switchInteractionSource
                )
            },
            modifier = Modifier.clickable(
                enabled = enabled && !isProcessing,
                interactionSource = listItemInteractionSource,
                onClick = { handleChange(!checked) }
            ),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * 深色模式选择器
 * 复刻 Animeko 的主题预览面板样式
 */
@Composable
private fun DarkModeSelector(
    themeSettings: ThemeSettings,
    currentMode: DarkMode,
    onModeSelected: (DarkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        DarkModeOption(
            mode = DarkMode.LIGHT,
            selected = currentMode == DarkMode.LIGHT,
            onClick = { onModeSelected(DarkMode.LIGHT) },
            themeSettings = themeSettings
        )
        DarkModeOption(
            mode = DarkMode.DARK,
            selected = currentMode == DarkMode.DARK,
            onClick = { onModeSelected(DarkMode.DARK) },
            themeSettings = themeSettings
        )
        DarkModeOption(
            mode = DarkMode.AUTO,
            selected = currentMode == DarkMode.AUTO,
            onClick = { onModeSelected(DarkMode.AUTO) },
            themeSettings = themeSettings
        )
    }
}

/**
 * 单个深色模式选项
 */
@Composable
private fun ThemePreviewPanel(
    isDark: Boolean,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier
) {
    val colorScheme = appColorScheme(
        seedColor = themeSettings.seedColor,
        useDynamicTheme = themeSettings.useDynamicTheme,
        useBlackBackground = themeSettings.useBlackBackground,
        isDark = isDark
    )
    
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = colorScheme.background,
                        shape = RoundedCornerShape(9.dp)
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(colorScheme.tertiary)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.secondary,
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                )
                        )
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(8.dp)
                                .background(color = colorScheme.primaryContainer, shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(8.dp)
                                .background(color = colorScheme.secondaryContainer, shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(8.dp)
                                .background(color = colorScheme.tertiaryContainer, shape = CircleShape)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .background(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(color = colorScheme.primary, shape = CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .background(color = colorScheme.primaryContainer, shape = CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagonalMixedThemePreviewPanel(
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        ThemePreviewPanel(
            isDark = false,
            themeSettings = themeSettings,
            modifier = Modifier.fillMaxSize().clip(TopLeftDiagonalShape)
        )
        ThemePreviewPanel(
            isDark = true,
            themeSettings = themeSettings,
            modifier = Modifier.fillMaxSize().clip(BottomRightDiagonalShape)
        )
    }
}

private object TopLeftDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private object BottomRightDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
@Composable
private fun DarkModeOption(
    mode: DarkMode,
    selected: Boolean,
    onClick: () -> Unit,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        // 主题预览面板
        if (mode != DarkMode.AUTO) {
            ThemePreviewPanel(
                isDark = mode == DarkMode.DARK,
                themeSettings = themeSettings,
                modifier = Modifier.size(96.dp, 146.dp)
            )
        } else {
            DiagonalMixedThemePreviewPanel(
                themeSettings = themeSettings,
                modifier = Modifier.size(96.dp, 146.dp)
            )
        }
        
        // RadioButton 和文字
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = when (mode) {
                    DarkMode.LIGHT -> "浅色"
                    DarkMode.DARK -> "深色"
                    DarkMode.AUTO -> "自动"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 颜色选择器网格
 * 使用 FlowRow 自动换行，完全复刻 Animeko
 */
@Composable
private fun ColorPaletteGrid(
    colors: List<Color>,
    selectedColor: Color,
    enabled: Boolean,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            ColorButton(
                color = color,
                selected = color.value == selectedColor.value,
                enabled = enabled,
                onClick = { onColorSelect(color) },
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

