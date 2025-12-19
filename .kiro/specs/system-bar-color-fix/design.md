# Design Document

## Overview

本设计旨在解决 Android 应用中系统栏颜色配置不一致和闪烁问题。核心思路是将系统栏颜色配置逻辑集中到单一位置，确保 MainActivity 和 App.kt 使用相同的颜色计算逻辑，并优化配置时机以避免颜色闪烁。

## Architecture

### 当前架构问题

```
MainActivity.onCreate()
  └─> configureSystemBars() [使用 getColorScheme()]
  └─> setContent { App() }
        └─> ConfigureSystemBars() [使用 MaterialTheme.colorScheme]
        
MainActivity.onResume()
  └─> configureSystemBars() [再次配置，可能与 App 冲突]
```

问题：
1. MainActivity 和 App.kt 分别调用系统栏配置
2. onResume 时重复配置可能覆盖 Compose 的配置
3. 两处使用的 ColorScheme 可能不同步

### 新架构设计

```
MainActivity.onCreate()
  └─> enableEdgeToEdge() [仅启用边到边模式，使用默认样式]
  └─> setContent { App() }
        └─> ConfigureSystemBars() [统一配置点]
        
MainActivity.onResume()
  └─> [不再配置系统栏]
```

优势：
1. 单一配置点，避免冲突
2. 利用 Compose 的响应式特性自动更新
3. 主题变化时自动同步系统栏颜色

## Components and Interfaces

### 1. SystemBarConfigurator (新增)

集中管理系统栏颜色配置的工具类。

```kotlin
object SystemBarConfigurator {
    /**
     * 将 Compose Color 转换为 Android Color Int
     */
    fun Color.toAndroidColor(): Int {
        return android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
    
    /**
     * 根据主题配置创建状态栏样式
     */
    fun createStatusBarStyle(
        isDark: Boolean,
        statusBarColor: Color
    ): SystemBarStyle {
        val colorInt = statusBarColor.toAndroidColor()
        return if (isDark) {
            SystemBarStyle.dark(colorInt)
        } else {
            SystemBarStyle.light(colorInt, colorInt)
        }
    }
    
    /**
     * 创建导航栏样式（始终透明）
     */
    fun createNavigationBarStyle(isDark: Boolean): SystemBarStyle {
        return if (isDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        }
    }
}
```

### 2. ConfigureSystemBars (修改)

简化 Composable 函数，使用 SystemBarConfigurator。

```kotlin
@Composable
actual fun ConfigureSystemBars(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val activity = LocalContext.current as? ComponentActivity
    
    if (activity != null) {
        DisposableEffect(isDark, statusBarColor) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarConfigurator.createStatusBarStyle(
                    isDark = isDark,
                    statusBarColor = statusBarColor
                ),
                navigationBarStyle = SystemBarConfigurator.createNavigationBarStyle(isDark)
            )
            onDispose { }
        }
    }
}
```

### 3. MainActivity (修改)

移除 onResume 中的系统栏配置，简化 onCreate。

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化各种管理器...
        
        // 仅启用边到边模式，使用默认样式
        // 实际颜色由 App.kt 中的 ConfigureSystemBars 控制
        enableEdgeToEdge()
        
        setContent {
            // 应用内容...
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 移除 configureSystemBars() 调用
        updateViewModel?.checkAndInstallIfReady()
    }
}
```

### 4. App.kt (保持不变)

继续使用 ConfigureSystemBars，但确保颜色一致性。

```kotlin
@Composable
fun App(openPlayerAudioId: Long? = null) {
    // 主题加载逻辑...
    
    AppTheme(themeSettings = currentThemeSettings) {
        // 统一的系统栏配置点
        ConfigureSystemBars(
            isDark = when (currentThemeSettings.darkMode) {
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
                DarkMode.AUTO -> isSystemInDarkTheme()
            },
            statusBarColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            navigationBarColor = Color.Transparent // 统一使用透明
        )
        
        // 应用内容...
    }
}
```

## Data Models

### SystemBarColors (新增)

用于传递系统栏颜色配置的数据类。

```kotlin
data class SystemBarColors(
    val statusBarColor: Color,
    val navigationBarColor: Color,
    val isDark: Boolean
) {
    companion object {
        fun from(colorScheme: ColorScheme, isDark: Boolean): SystemBarColors {
            return SystemBarColors(
                statusBarColor = colorScheme.surfaceContainerLowest,
                navigationBarColor = Color.Transparent,
                isDark = isDark
            )
        }
    }
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Theme-to-SystemBar Color Consistency

*For any* theme configuration (dark/light mode, seed color, dynamic theme setting), when the application starts or theme changes, the system bar colors should match the theme's surfaceContainerLowest color.

**Validates: Requirements 1.1, 1.2, 1.3, 1.5**

### Property 2: Navigation Bar Transparency Invariant

*For any* theme configuration and application state, the navigation bar color should always be transparent.

**Validates: Requirements 1.4**

### Property 3: Lifecycle Color Consistency

*For any* application lifecycle event (resume from background, app switching), the system bar colors before and after the event should remain identical if the theme has not changed.

**Validates: Requirements 2.1, 2.4**

### Property 4: Mode Switch Completeness

*For any* theme mode switch (light to dark or dark to light), both the system bar color and icon style should update to match the new mode.

**Validates: Requirements 3.2**

### Property 5: Dynamic Theme Propagation

*For any* change to dynamic theme setting (enabled/disabled), the system bar colors should reflect the new color scheme derived from the dynamic theme state.

**Validates: Requirements 3.3**

### Property 6: Seed Color Propagation

*For any* seed color change, the system bar color should derive from the new seed color's generated color scheme.

**Validates: Requirements 3.4**

### Property 7: ColorScheme Calculation Consistency

*For any* given theme settings (seed color, dark mode, dynamic theme, black background), calculating the ColorScheme through different code paths should produce identical results.

**Validates: Requirements 4.3**

### Property 8: Theme Change Propagation

*For any* theme settings change, the system bar configuration should be updated to reflect the new theme within one frame.

**Validates: Requirements 4.4**

### Property 9: Intent Recreation Color Consistency

*For any* new Intent that triggers Activity recreation, the system bar colors should remain consistent with the current theme throughout the recreation process.

**Validates: Requirements 5.3**

## Error Handling

### Theme Loading Failures

**Scenario**: Theme settings fail to load from storage.

**Handling**:
1. Use sensible default theme settings (light mode, default seed color)
2. Log the error for debugging
3. Continue with default configuration
4. Retry loading on next app start

```kotlin
private fun getDefaultThemeSettings(): ThemeSettings {
    return ThemeSettings(
        darkMode = DarkMode.AUTO,
        seedColor = Color(0xFF6750A4), // Material Purple
        useDynamicTheme = false,
        useBlackBackground = false
    )
}
```

### Activity Context Unavailable

**Scenario**: ConfigureSystemBars is called but Activity context is not available.

**Handling**:
1. Silently skip system bar configuration
2. System bars will use default Android styling
3. Configuration will be retried when Activity becomes available

### ColorScheme Calculation Errors

**Scenario**: Exception occurs during ColorScheme calculation.

**Handling**:
1. Catch exception and log error
2. Fall back to Material default color scheme
3. Use default system bar styling

## Testing Strategy

### Unit Testing

We will write unit tests for:

1. **SystemBarConfigurator utility functions**:
   - Color conversion (Compose Color to Android Color Int)
   - SystemBarStyle creation for different modes
   - Edge cases (fully transparent, fully opaque colors)

2. **SystemBarColors data class**:
   - Factory method creates correct colors from ColorScheme
   - Dark/light mode handling

3. **Theme settings to system bar color mapping**:
   - Verify correct colors for different theme configurations
   - Test dynamic theme color extraction

### Property-Based Testing

We will use Kotest property testing library for Kotlin to verify universal properties.

**Configuration**: Each property test will run a minimum of 100 iterations with randomly generated inputs.

**Property Test Structure**:
```kotlin
class SystemBarColorPropertyTest : StringSpec({
    "Property 1: Theme-to-SystemBar Color Consistency" {
        checkAll(100, themeConfigGenerator) { themeConfig ->
            val colorScheme = calculateColorScheme(themeConfig)
            val systemBarColors = SystemBarColors.from(colorScheme, themeConfig.isDark)
            
            systemBarColors.statusBarColor shouldBe colorScheme.surfaceContainerLowest
        }
    }
})
```

**Generators**:
- `themeConfigGenerator`: Generates random theme configurations
- `seedColorGenerator`: Generates random seed colors
- `lifecycleEventGenerator`: Generates random lifecycle event sequences

### Integration Testing

We will write integration tests for:

1. **App startup flow**:
   - Verify system bar colors are set correctly on first launch
   - Test with different saved theme preferences

2. **Theme switching flow**:
   - Change theme settings and verify system bar updates
   - Test all theme setting combinations

3. **Lifecycle event handling**:
   - Simulate app going to background and returning
   - Verify no color flickering or inconsistency

### Manual Testing Checklist

- [ ] Launch app and verify system bar matches theme
- [ ] Switch to dark mode and verify system bar updates
- [ ] Switch to light mode and verify system bar updates
- [ ] Enable dynamic theme and verify system bar reflects wallpaper colors
- [ ] Change seed color and verify system bar updates
- [ ] Send app to background and return, verify no flickering
- [ ] Open app from notification, verify correct colors
- [ ] Rotate device, verify colors remain consistent
- [ ] Test on Android 12+ with dynamic theme
- [ ] Test on Android 11 and below without dynamic theme

## Implementation Notes

### Migration Strategy

1. **Phase 1**: Add SystemBarConfigurator utility
2. **Phase 2**: Update ConfigureSystemBars to use new utility
3. **Phase 3**: Simplify MainActivity, remove duplicate configuration
4. **Phase 4**: Test thoroughly on different Android versions
5. **Phase 5**: Monitor for any regressions

### Performance Considerations

- System bar configuration is lightweight, no performance concerns
- DisposableEffect ensures configuration only runs when colors change
- Avoid unnecessary recompositions by using stable color values

### Compatibility

- Minimum Android API: 21 (Lollipop)
- Edge-to-edge mode: API 21+
- Dynamic theme: API 31+ (Android 12)
- All features gracefully degrade on older versions

### Known Limitations

1. System bar color changes are not animated by the system
2. Some Android OEM skins may override system bar styling
3. Gesture navigation bar (Android 10+) may have different appearance

## Alternative Approaches Considered

### Approach 1: Keep Dual Configuration

**Description**: Keep both MainActivity and App.kt configuring system bars, but synchronize them.

**Pros**: 
- Ensures system bars are configured even if Compose fails
- Faster initial configuration

**Cons**:
- Complex synchronization logic
- Risk of race conditions
- Code duplication

**Decision**: Rejected due to complexity and maintenance burden.

### Approach 2: Configuration in MainActivity Only

**Description**: Configure system bars only in MainActivity, remove from App.kt.

**Pros**:
- Single configuration point
- No Compose dependency

**Cons**:
- Doesn't react to theme changes automatically
- Requires manual updates when theme changes
- Loses Compose's reactive benefits

**Decision**: Rejected because we want automatic updates with theme changes.

### Approach 3: Shared ViewModel for System Bar State

**Description**: Create a ViewModel to manage system bar state, shared between MainActivity and App.kt.

**Pros**:
- Clear state management
- Easy to test

**Cons**:
- Overkill for simple color configuration
- Adds unnecessary complexity
- Still requires coordination between two configuration points

**Decision**: Rejected as over-engineered for this use case.

## Future Enhancements

1. **Animated Transitions**: Add custom animations for system bar color changes
2. **Per-Screen Colors**: Allow different screens to customize system bar colors
3. **Status Bar Content Color**: Dynamically adjust status bar icon colors based on background luminance
4. **Immersive Mode Support**: Handle system bar visibility changes in immersive mode
