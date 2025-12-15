# 设计文档 - 多功能媒体播放应用

## 概述

本应用是一个跨平台媒体播放和下载管理系统，基于 Kotlin Multiplatform + Compose Multiplatform 框架。应用采用模块化架构，分离关注点，支持有声书播放、视频播放和文件下载功能。

### 核心特性

- **多格式播放**：支持有声书（MP3、M4A 等）和视频（MP4、MKV 等）
- **自适应 UI**：根据设备类型自动调整布局（移动端底部导航、桌面端左侧导航）
- **主题定制**：支持多种主题色、深色/浅色模式、多语言
- **离线支持**：下载管理和播放进度本地保存
- **跨平台**：Android、Desktop、iOS 统一代码库

## 架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                    UI 层 (Compose)                   │
│  ┌──────────────┬──────────────┬──────────────┐    │
│  │ AudiobookUI  │   VideoUI    │ DownloadsUI  │    │
│  └──────────────┴──────────────┴──────────────┘    │
│  ┌──────────────────────────────────────────────┐  │
│  │         Settings & Navigation                │  │
│  └──────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│              Domain 层 (业务逻辑)                    │
│  ┌──────────────┬──────────────┬──────────────┐    │
│  │ AudiobookVM  │   VideoVM    │ DownloadsVM  │    │
│  └──────────────┴──────────────┴──────────────┘    │
├─────────────────────────────────────────────────────┤
│              Data 层 (数据管理)                      │
│  ┌──────────────┬──────────────┬──────────────┐    │
│  │ AudiobookRepo│   VideoRepo  │DownloadsRepo│    │
│  └──────────────┴──────────────┴──────────────┘    │
├─────────────────────────────────────────────────────┤
│         基础设施 (Database, File, Network)          │
│  ┌──────────────┬──────────────┬──────────────┐    │
│  │   Room DB    │  FileManager │DownloadMgr  │    │
│  └──────────────┴──────────────┴──────────────┘    │
└─────────────────────────────────────────────────────┘
```

### 模块结构

```
app/shared/src/commonMain/kotlin/
├── ui/
│   ├── main/
│   │   ├── AudiobookScreen.kt
│   │   ├── VideoScreen.kt
│   │   └── DownloadsScreen.kt
│   ├── settings/
│   │   ├── ThemeSettingsPage.kt
│   │   ├── audiobook/
│   │   │   └── AudiobookMenuDialog.kt
│   │   ├── video/
│   │   │   └── VideoMenuDialog.kt
│   │   └── downloads/
│   │       └── DownloadsMenuDialog.kt
│   ├── components/
│   │   ├── player/
│   │   │   ├── AudioPlayerControls.kt
│   │   │   ├── VideoPlayerControls.kt
│   │   │   └── ProgressBar.kt
│   │   ├── download/
│   │   │   ├── DownloadItem.kt
│   │   │   └── DownloadProgressBar.kt
│   │   └── common/
│   │       └── MediaList.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   └── ThemeColors.kt
│   └── adaptive/
│       └── AniNavigationSuiteScaffold.kt
├── domain/
│   ├── audiobook/
│   │   ├── models/
│   │   │   ├── Audiobook.kt
│   │   │   └── PlaybackState.kt
│   │   ├── repositories/
│   │   │   └── AudiobookRepository.kt
│   │   └── usecases/
│   │       ├── GetAudiobooksUseCase.kt
│   │       └── SavePlaybackProgressUseCase.kt
│   ├── video/
│   │   ├── models/
│   │   │   ├── Video.kt
│   │   │   └── PlaybackState.kt
│   │   ├── repositories/
│   │   │   └── VideoRepository.kt
│   │   └── usecases/
│   │       ├── GetVideosUseCase.kt
│   │       └── SavePlaybackProgressUseCase.kt
│   ├── download/
│   │   ├── models/
│   │   │   ├── DownloadTask.kt
│   │   │   └── DownloadState.kt
│   │   ├── repositories/
│   │   │   └── DownloadRepository.kt
│   │   └── usecases/
│   │       ├── CreateDownloadUseCase.kt
│   │       ├── PauseDownloadUseCase.kt
│   │       └── ResumeDownloadUseCase.kt
│   └── settings/
│       ├── models/
│       │   └── AppSettings.kt
│       ├── repositories/
│       │   └── SettingsRepository.kt
│       └── usecases/
│           └── UpdateSettingsUseCase.kt
└── data/
    ├── audiobook/
    │   ├── local/
    │   │   ├── AudiobookDao.kt
    │   │   └── AudiobookEntity.kt
    │   └── AudiobookRepositoryImpl.kt
    ├── video/
    │   ├── local/
    │   │   ├── VideoDao.kt
    │   │   └── VideoEntity.kt
    │   └── VideoRepositoryImpl.kt
    ├── download/
    │   ├── local/
    │   │   ├── DownloadDao.kt
    │   │   └── DownloadEntity.kt
    │   └── DownloadRepositoryImpl.kt
    └── settings/
        ├── local/
        │   └── SettingsDataStore.kt
        └── SettingsRepositoryImpl.kt
```

## 组件和接口

### 1. 播放器组件

#### AudioPlayerControls
- 显示播放/暂停按钮
- 显示进度条和时间戳
- 支持快进/快退
- 支持播放速度调整

#### VideoPlayerControls
- 显示播放/暂停按钮
- 显示进度条和时间戳
- 支持全屏切换
- 支持清晰度选择

### 2. 下载组件

#### DownloadItem
- 显示文件名和下载进度
- 显示下载速度和剩余时间
- 提供暂停/恢复/取消按钮

#### DownloadProgressBar
- 显示下载进度百分比
- 显示已下载/总大小

### 3. 导航组件

#### MainNavigation
- 自适应导航（底部 Tab / 左侧 Rail）
- 三个主要 Tab：有声书、视频、下载
- 下载页面右上角设置按钮

## 数据模型

### 有声书模型

```kotlin
data class Audiobook(
    val id: String,
    val title: String,
    val author: String,
    val filePath: String,
    val duration: Long,
    val coverUrl: String?,
    val createdAt: Long
)

data class AudiobookPlaybackState(
    val audiobookId: String,
    val currentPosition: Long,
    val duration: Long,
    val playbackSpeed: Float = 1.0f,
    val lastPlayedAt: Long
)
```

### 视频模型

```kotlin
data class Video(
    val id: String,
    val title: String,
    val filePath: String,
    val duration: Long,
    val thumbnailPath: String?,
    val createdAt: Long
)

data class VideoPlaybackState(
    val videoId: String,
    val currentPosition: Long,
    val duration: Long,
    val quality: VideoQuality = VideoQuality.AUTO,
    val lastPlayedAt: Long
)

enum class VideoQuality {
    AUTO, LOW, MEDIUM, HIGH
}
```

### 下载模型

```kotlin
data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val state: DownloadState,
    val speed: Long,
    val createdAt: Long
)

enum class DownloadState {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}
```

### 设置模型

```kotlin
data class AppSettings(
    val darkMode: DarkMode,
    val seedColor: Color,
    val useDynamicTheme: Boolean,
    val useBlackBackground: Boolean,
    val language: Language
)

enum class DarkMode {
    AUTO, LIGHT, DARK
}

enum class Language {
    ENGLISH, CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL
}
```

## 正确性属性

*属性是系统应该在所有有效执行中保持为真的特征或行为——本质上是关于系统应该做什么的正式陈述。属性充当人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1：播放进度持久化
对于任何有声书或视频，当用户停止播放时，系统应该将当前播放位置保存到本地存储。当应用重启并用户再次打开同一媒体文件时，播放应该从保存的位置继续。

**验证需求：1.5, 2.5, 7.1**

### 属性 2：下载任务状态一致性
对于任何下载任务，任务的当前状态应该与其实际的文件系统状态一致。如果任务标记为已完成，则文件应该存在于指定位置；如果标记为已取消，则不完整的文件应该被删除。

**验证需求：3.5, 3.6, 7.2**

### 属性 3：主题设置立即生效
对于任何主题设置的修改（主题色、深色模式、语言），修改应该立即应用到整个应用界面，无需重启应用。

**验证需求：4.2, 4.3, 4.4**

### 属性 4：设置持久化和恢复
对于任何用户修改的设置，修改应该被持久化保存到本地存储。当应用重启时，所有设置应该被恢复到用户上次保存的状态。

**验证需求：4.5, 4.6**

### 属性 5：导航状态保持
对于任何导航操作，当用户从一个页面导航到另一个页面再返回时，原页面的滚动位置和列表状态应该被保持。

**验证需求：6.2, 6.5**

### 属性 6：下载暂停恢复一致性
对于任何暂停的下载任务，当用户恢复下载时，系统应该从暂停位置继续下载，而不是重新开始。恢复后的下载应该最终产生与完整下载相同的文件。

**验证需求：3.3, 3.4**

### 属性 7：播放列表完整性
对于任何媒体列表（有声书或视频），列表中显示的项目数应该与实际存储的媒体文件数相匹配。删除媒体文件后，列表应该自动更新。

**验证需求：7.3, 7.4**

## 错误处理

### 播放器错误
- 文件不存在：显示错误提示，返回列表
- 格式不支持：显示不支持的格式提示
- 播放失败：显示重试选项

### 下载错误
- 网络错误：自动重试或显示重试按钮
- 存储空间不足：显示存储空间不足提示
- 文件写入失败：显示错误提示并清理不完整文件

### 数据库错误
- 数据库损坏：清理缓存并重新初始化
- 查询失败：显示加载失败提示

## 测试策略

### 单元测试

- 播放进度计算逻辑
- 下载速度计算
- 设置序列化/反序列化
- 数据模型验证

### 属性测试

使用 Kotest 或 QuickCheck 进行属性测试：

1. **播放进度持久化属性测试**
   - 生成随机播放位置
   - 保存并恢复，验证一致性

2. **下载任务状态一致性属性测试**
   - 生成随机下载任务
   - 执行各种操作，验证状态与文件系统一致

3. **主题设置立即生效属性测试**
   - 生成随机主题设置
   - 应用设置，验证 UI 立即更新

4. **设置持久化属性测试**
   - 生成随机设置值
   - 保存、重启、恢复，验证一致性

5. **下载暂停恢复一致性属性测试**
   - 生成随机下载任务
   - 暂停、恢复、完成，验证最终文件一致性

### 集成测试

- 完整的播放流程（选择 → 播放 → 暂停 → 恢复 → 停止）
- 完整的下载流程（创建 → 下载 → 暂停 → 恢复 → 完成）
- 设置修改和应用流程
- 导航和页面切换流程

### UI 测试

- 播放器控制按钮功能
- 进度条拖动
- 下载列表交互
- 设置页面交互
