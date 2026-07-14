package com.xmvisio.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun VideoPlayerPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val manager = remember { VideoPlayerPreferencesManager.getInstance(context) }
    val prefs by manager.preferences.collectAsState()

    // 弹窗状态
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLoopModeDialog by remember { mutableStateOf(false) }
    var showDoubleTapDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showScaleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频播放器设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // === 播放 ===
            SectionTitle("播放")

            SettingsCardGroup {
                VpSettingsCardItem(
                    icon = Icons.Default.Speed,
                    title = "默认播放速度",
                    subtitle = "${prefs.defaultPlaybackSpeed}x",
                    onClick = { showSpeedDialog = true },
                    isFirst = true
                )

                VpSettingsSwitchItem(
                    icon = Icons.Default.PlayArrow,
                    title = "自动播放",
                    subtitle = "进入视频后自动开始播放",
                    checked = prefs.autoplay,
                    onCheckedChange = { manager.setAutoplay(it) }
                )

                VpSettingsCardItem(
                    icon = Icons.Default.Repeat,
                    title = "循环模式",
                    subtitle = when (prefs.loopMode) {
                        LoopMode.OFF -> "关闭"
                        LoopMode.ONE -> "单曲循环"
                        LoopMode.ALL -> "列表循环"
                    },
                    onClick = { showLoopModeDialog = true }
                )

                VpSettingsSwitchItem(
                    icon = Icons.Default.History,
                    title = "恢复播放",
                    subtitle = "从上次中断处继续播放",
                    checked = prefs.resumePlayback,
                    onCheckedChange = { manager.setResumePlayback(it) },
                    isLast = true
                )
            }

            // === 手势控制 ===
            SectionTitle("手势控制")

            SettingsCardGroup {
                VpSettingsSwitchItem(
                    icon = Icons.Default.SwapHoriz,
                    title = "滑动 seek",
                    subtitle = "左右滑动调整进度",
                    checked = prefs.enableSeekGesture,
                    onCheckedChange = { manager.setEnableSeekGesture(it) },
                    isFirst = true
                )

                if (prefs.enableSeekGesture) {
                    VpSettingsSliderItem(
                        icon = Icons.Default.Tune,
                        title = "seek 灵敏度",
                        value = prefs.seekSensitivity,
                        valueRange = 0.1f..2.0f,
                        onValueChange = { manager.setSeekSensitivity(it) }
                    )
                }

                VpSettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    title = "右侧垂直滑动：音量",
                    subtitle = "右侧上下滑动调音量",
                    checked = prefs.enableVolumeGesture,
                    onCheckedChange = { manager.setEnableVolumeGesture(it) }
                )

                if (prefs.enableVolumeGesture) {
                    VpSettingsSliderItem(
                        icon = Icons.Default.Tune,
                        title = "音量灵敏度",
                        value = prefs.volumeSensitivity,
                        valueRange = 0.1f..2.0f,
                        onValueChange = { manager.setVolumeSensitivity(it) }
                    )
                }

                VpSettingsSwitchItem(
                    icon = Icons.Default.Brightness6,
                    title = "左侧垂直滑动：亮度",
                    subtitle = "左侧上下滑动调亮度",
                    checked = prefs.enableBrightnessGesture,
                    onCheckedChange = { manager.setEnableBrightnessGesture(it) }
                )

                if (prefs.enableBrightnessGesture) {
                    VpSettingsSliderItem(
                        icon = Icons.Default.Tune,
                        title = "亮度灵敏度",
                        value = prefs.brightnessSensitivity,
                        valueRange = 0.1f..2.0f,
                        onValueChange = { manager.setBrightnessSensitivity(it) }
                    )
                }

                VpSettingsCardItem(
                    icon = Icons.Default.TouchApp,
                    title = "双击手势",
                    subtitle = when (prefs.doubleTapGesture) {
                        DoubleTapGesture.PLAY_PAUSE -> "播放/暂停"
                        DoubleTapGesture.FAST_FORWARD_AND_REWIND -> "快进/快退"
                        DoubleTapGesture.BOTH -> "快进/快退 + 播放/暂停"
                        DoubleTapGesture.NONE -> "关闭"
                    },
                    onClick = { showDoubleTapDialog = true }
                )

                VpSettingsSwitchItem(
                    icon = Icons.Default.PushPin,
                    title = "长按快进",
                    subtitle = "长按屏幕快进",
                    checked = prefs.enableLongPress,
                    onCheckedChange = { manager.setEnableLongPress(it) }
                )

                if (prefs.enableLongPress) {
                    VpSettingsSliderItem(
                        icon = Icons.Default.Speed,
                        title = "长按速度",
                        value = prefs.longPressSpeed,
                        valueRange = 0.5f..4.0f,
                        onValueChange = { manager.setLongPressSpeed(it) }
                    )
                }

                VpSettingsSliderItem(
                    icon = Icons.Default.FastForward,
                    title = "快进/快退增量",
                    value = prefs.seekIncrement.toFloat(),
                    valueRange = 1f..60f,
                    onValueChange = { manager.setSeekIncrement(it.toInt()) },
                    valueText = "${prefs.seekIncrement} 秒",
                    isLast = true
                )
            }

            // === 界面 ===
            SectionTitle("界面")

            SettingsCardGroup {
                VpSettingsSliderItem(
                    icon = Icons.Default.Timer,
                    title = "控件自动隐藏",
                    value = prefs.controllerAutoHideTimeout.toFloat(),
                    valueRange = 1f..30f,
                    onValueChange = { manager.setControllerAutoHideTimeout(it.toInt()) },
                    valueText = "${prefs.controllerAutoHideTimeout} 秒",
                    isFirst = true
                )

                VpSettingsCardItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "屏幕方向",
                    subtitle = when (prefs.screenOrientation) {
                        ScreenOrientation.AUTOMATIC -> "自动"
                        ScreenOrientation.LANDSCAPE -> "横屏"
                        ScreenOrientation.LANDSCAPE_REVERSE -> "反向横屏"
                        ScreenOrientation.PORTRAIT -> "竖屏"
                        ScreenOrientation.VIDEO_ORIENTATION -> "跟随视频"
                    },
                    onClick = { showOrientationDialog = true }
                )

                VpSettingsCardItem(
                    icon = Icons.Default.AspectRatio,
                    title = "视频缩放",
                    subtitle = when (prefs.videoContentScale) {
                        VideoContentScale.BEST_FIT -> "最佳适配"
                        VideoContentScale.STRETCH -> "拉伸"
                        VideoContentScale.CROP -> "裁剪"
                        VideoContentScale.HUNDRED_PERCENT -> "100%"
                    },
                    onClick = { showScaleDialog = true },
                    isLast = true
                )
            }
        }
    }

    // === 弹窗 ===

    // 播放速度选择
    if (showSpeedDialog) {
        SingleChoiceDialog(
            title = "默认播放速度",
            options = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f),
            selected = prefs.defaultPlaybackSpeed,
            label = { "${it}x" },
            onSelect = { manager.setDefaultPlaybackSpeed(it) },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // 循环模式选择
    if (showLoopModeDialog) {
        SingleChoiceDialog(
            title = "循环模式",
            options = LoopMode.entries,
            selected = prefs.loopMode,
            label = {
                when (it) {
                    LoopMode.OFF -> "关闭"
                    LoopMode.ONE -> "单曲循环"
                    LoopMode.ALL -> "列表循环"
                }
            },
            onSelect = { manager.setLoopMode(it) },
            onDismiss = { showLoopModeDialog = false }
        )
    }

    // 双击手势选择
    if (showDoubleTapDialog) {
        SingleChoiceDialog(
            title = "双击手势",
            options = DoubleTapGesture.entries,
            selected = prefs.doubleTapGesture,
            label = {
                when (it) {
                    DoubleTapGesture.PLAY_PAUSE -> "播放/暂停"
                    DoubleTapGesture.FAST_FORWARD_AND_REWIND -> "快进/快退"
                    DoubleTapGesture.BOTH -> "快进/快退 + 播放/暂停"
                    DoubleTapGesture.NONE -> "关闭"
                }
            },
            onSelect = { manager.setDoubleTapGesture(it) },
            onDismiss = { showDoubleTapDialog = false }
        )
    }

    // 屏幕方向选择
    if (showOrientationDialog) {
        SingleChoiceDialog(
            title = "屏幕方向",
            options = ScreenOrientation.entries,
            selected = prefs.screenOrientation,
            label = {
                when (it) {
                    ScreenOrientation.AUTOMATIC -> "自动"
                    ScreenOrientation.LANDSCAPE -> "横屏"
                    ScreenOrientation.LANDSCAPE_REVERSE -> "反向横屏"
                    ScreenOrientation.PORTRAIT -> "竖屏"
                    ScreenOrientation.VIDEO_ORIENTATION -> "跟随视频"
                }
            },
            onSelect = { manager.setScreenOrientation(it) },
            onDismiss = { showOrientationDialog = false }
        )
    }

    // 视频缩放选择
    if (showScaleDialog) {
        SingleChoiceDialog(
            title = "视频缩放",
            options = VideoContentScale.entries,
            selected = prefs.videoContentScale,
            label = {
                when (it) {
                    VideoContentScale.BEST_FIT -> "最佳适配"
                    VideoContentScale.STRETCH -> "拉伸"
                    VideoContentScale.CROP -> "裁剪"
                    VideoContentScale.HUNDRED_PERCENT -> "100%"
                }
            },
            onSelect = { manager.setVideoContentScale(it) },
            onDismiss = { showScaleDialog = false }
        )
    }
}

// === 通用单选弹窗 ===

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            },
                        color = if (option == selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = option == selected,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                }
                            )
                            Text(
                                text = label(option),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (option == selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// === 辅助组件 ===

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun VpSettingsCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(24.dp)
        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VpSettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(24.dp)
        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun VpSettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(24.dp)
        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                }

                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                Text(
                    text = valueText ?: String.format("%.1f", value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
            )
        }
    }
}
