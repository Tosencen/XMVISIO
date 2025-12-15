# XMVISIO

一套代码，多端运行：Android、Desktop (Windows/Mac/Linux)

基于 Kotlin Multiplatform + Compose Multiplatform 构建的跨平台媒体应用。


## ✨ 特性

- 🎯 **Kotlin Multiplatform** - 共享业务逻辑
- 🎨 **Compose Multiplatform** - 统一 UI 框架
- 📱 **Android** - 原生 Android 应用，支持自适应图标
- 🖥️ **Desktop** - macOS/Windows/Linux 桌面应用
- 🎨 **自适应导航** - 桌面端侧边栏，移动端底部导航栏
- 🌓 **主题系统** - 支持浅色/深色/自动主题切换
- 🧩 **模块化架构** - 清晰的分层设计
- 🚀 **开箱即用** - 预配置构建系统

## 📋 环境要求

### 必需
- **JDK 21+** (推荐 JetBrains Runtime)
- **Android Studio Ladybug (2024.2.1)** 或更高版本
- **Gradle 8.13+** (项目自带 wrapper)

### Android 开发
- Android SDK API 27-35
- 配置 `local.properties`:
  ```properties
  sdk.dir=/path/to/your/Android/sdk
  ```

### Desktop 开发
- macOS 13+ (macOS 应用)
- Windows 10+ (Windows 应用)
- Linux (Linux 应用)

## 🚀 快速开始

### 1️⃣ 克隆项目

```bash
git clone <repository-url>
cd <project-directory>
```

### 2️⃣ 配置 Android SDK

创建 `local.properties` 文件：
```bash
echo "sdk.dir=/Users/你的用户名/Library/Android/sdk" > local.properties
```

### 3️⃣ 运行项目

#### 🤖 Android
```bash
# 构建 Debug APK
./gradlew :app:android:assembleDebug

# 安装到设备
./gradlew :app:android:installDebug

# 或在 Android Studio 中点击 Run
```

#### 🖥️ Desktop
```bash
# 直接运行
./gradlew :app:desktop:run

# 打包 DMG (macOS)
./gradlew :app:desktop:packageDmg

# 打包当前平台应用
./gradlew :app:desktop:packageDistributionForCurrentOS
```

## 📁 项目结构

```
XMVISIO/
├── app/
│   ├── shared/                          # 共享代码（核心业务逻辑和 UI）
│   │   ├── src/
│   │   │   ├── commonMain/              # 所有平台共享
│   │   │   │   ├── kotlin/
│   │   │   │   │   ├── ui/
│   │   │   │   │   │   ├── adaptive/   # 自适应导航组件
│   │   │   │   │   │   │   ├── AniNavigationSuiteScaffold.kt
│   │   │   │   │   │   │   └── AniNavigationSuite.kt
│   │   │   │   │   │   ├── main/       # 主界面
│   │   │   │   │   │   ├── settings/   # 设置页面
│   │   │   │   │   │   └── theme/      # 主题系统
│   │   │   │   │   ├── data/           # 数据模型
│   │   │   │   │   └── App.kt          # 应用入口
│   │   │   ├── androidMain/             # Android 特定
│   │   │   │   └── kotlin/ui/
│   │   │   │       ├── DesktopWindowInsets.android.kt
│   │   │   │       └── SystemBars.android.kt
│   │   │   └── desktopMain/             # Desktop 特定
│   │   │       └── kotlin/ui/
│   │   │           ├── DesktopWindowInsets.desktop.kt
│   │   │           └── SystemBars.desktop.kt
│   │   └── build.gradle.kts
│   ├── android/                         # Android 启动器
│   │   ├── src/main/
│   │   │   ├── kotlin/MainActivity.kt
│   │   │   ├── AndroidManifest.xml
│   │   │   └── res/
│   │   │       ├── mipmap-*/            # 应用图标（各分辨率）
│   │   │       ├── drawable/            # 自适应图标前景
│   │   │       └── values/              # 颜色和主题
│   │   └── build.gradle.kts
│   └── desktop/                         # Desktop 启动器
│       ├── src/jvmMain/kotlin/Main.kt
│       ├── icons/
│       │   ├── icon.icns                # macOS 图标
│       │   ├── icon_macos.svg           # macOS 图标源文件
│       │   └── a_1024x1024_rounded.ico  # Windows 图标
│       └── build.gradle.kts
├── core/
│   └── utils/                           # 核心工具模块
│       ├── platform/                    # 平台适配层
│       ├── logging/                     # 日志系统
│       ├── coroutines/                  # 协程工具
│       └── serialization/               # 序列化工具
├── buildSrc/                            # 构建逻辑和插件
├── build.gradle.kts                     # 根构建文件
├── settings.gradle.kts                  # 项目设置
└── gradle.properties                    # 全局配置
```

## 🔧 配置说明

### 应用信息

- **应用名称**: XMVISIO
- **包名**: `com.template.kmp`
- **版本**: 见 `gradle.properties`

### 修改应用信息

1. **包名和应用 ID**
   - `app/android/build.gradle.kts` → `applicationId`
   - `app/shared/build.gradle.kts` → `namespace`
   - 所有代码文件的包名

2. **应用名称**
   - Android: `app/android/src/main/AndroidManifest.xml` → `android:label`
   - Desktop: `app/desktop/src/jvmMain/kotlin/Main.kt` → 窗口标题
   - macOS Dock: `app/desktop/build.gradle.kts` → `dockName`

3. **版本号**
   - `gradle.properties` → `version.name` 和 `android.version.code`

### 应用图标

#### Android
- 使用自适应图标系统 (API 26+)
- 前景图标: `app/android/src/main/res/mipmap-*/ic_launcher_foreground.png`
- 支持主题自适应: 浅色模式黑色图标，深色模式白色图标
- 配置文件: `app/android/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

#### macOS
- 图标文件: `app/desktop/icons/icon.icns`
- 源文件: `app/desktop/icons/icon_macos.svg`
- 符合 macOS 设计规范（1024x1024，圆角矩形背景）

#### Windows
- 图标文件: `app/desktop/icons/a_1024x1024_rounded.ico`

### 核心功能

#### 自适应导航
- **桌面端**: 左侧 NavigationRail，支持 macOS 标题栏按钮避让
- **移动端**: 底部 NavigationBar
- 自动响应窗口大小变化
- 实现文件: `app/shared/src/commonMain/kotlin/ui/adaptive/`

#### 主题系统
- 支持浅色/深色/自动三种模式
- Material 3 Design
- 配置文件: `app/shared/src/commonMain/kotlin/ui/theme/`

#### 平台特定适配
- macOS 标题栏 insets 处理
- Android 系统栏颜色配置
- 实现文件: `app/shared/src/{platform}Main/kotlin/ui/`

### 添加依赖

在 `app/shared/build.gradle.kts` 的 `commonMain.dependencies` 中添加：
```kotlin
commonMain.dependencies {
    // 网络请求
    implementation("io.ktor:ktor-client-core:3.0.1")
    
    // 数据库
    implementation("androidx.room:room-runtime:2.7.0")
    
    // 依赖注入
    implementation("io.insert-koin:koin-core:4.0.0")
}
```

## 📦 构建发布版本

### Android APK/AAB
```bash
# Debug APK（输出到桌面）
./gradlew :app:android:assembleDebug
cp app/android/build/outputs/apk/debug/android-debug.apk ~/Desktop/

# Release APK（需要签名配置）
./gradlew :app:android:assembleRelease

# Android App Bundle
./gradlew :app:android:bundleRelease
```

### Desktop 应用包

#### macOS DMG
```bash
# 清理构建缓存
./gradlew clean

# 打包 DMG
./gradlew :app:desktop:packageDmg

# 移动到桌面
cp app/desktop/build/compose/binaries/main/dmg/XMVISIO-*.dmg ~/Desktop/
```

#### 其他平台
```bash
# 打包当前平台
./gradlew :app:desktop:packageDistributionForCurrentOS

# 生成的文件在：
# app/desktop/build/compose/binaries/main/
```

## 🛠️ 常见问题

### Q: Android SDK 找不到？
**A:** 确保 `local.properties` 文件存在且路径正确：
```properties
sdk.dir=/Users/你的用户名/Library/Android/sdk
```

### Q: Gradle 构建失败？
**A:** 尝试清理缓存：
```bash
./gradlew clean
./gradlew --stop
rm -rf .gradle .kotlin
./gradlew build
```

### Q: Android 应用闪退？
**A:** 检查 logcat 日志：
```bash
adb logcat -s AndroidRuntime:E
```
确保所有平台特定的 CompositionLocal 都已正确提供。

### Q: macOS 图标不符合规范？
**A:** 使用以下命令从 SVG 生成 ICNS：
```bash
# 1. 从 SVG 生成 PNG
rsvg-convert -w 1024 -h 1024 icon_macos.svg -o icon_1024.png

# 2. 创建 iconset
mkdir icon.iconset
sips -z 16 16 icon_1024.png --out icon.iconset/icon_16x16.png
# ... (其他尺寸)

# 3. 生成 ICNS
iconutil -c icns icon.iconset -o icon.icns
```

### Q: 如何添加网络请求？
**A:** 在 `commonMain` 中使用 Ktor Client：
```kotlin
val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}
```

## 🎨 UI 组件

### 自适应导航组件
- `AniNavigationSuiteScaffold`: 主导航脚手架，自动适配桌面/移动布局
- `AniNavigationSuite`: 导航组件，支持 NavigationRail 和 NavigationBar
- 特性:
  - 自动响应窗口大小
  - macOS 标题栏按钮避让
  - 可自定义 header/footer
  - 可配置导航项间距

### 主题组件
- `AppTheme`: 应用主题容器
- `ThemeSettingsPage`: 主题设置页面
- 支持浅色/深色/自动模式切换

## 📚 学习资源

- [Kotlin Multiplatform 官方文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 文档](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Material 3 Design](https://m3.material.io/)
- [Animeko 源项目](https://github.com/open-ani/animeko)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

基于原项目 [Animeko](https://github.com/open-ani/animeko) 的 GNU AGPLv3 许可证。

---

**Happy Coding! 🎉**
