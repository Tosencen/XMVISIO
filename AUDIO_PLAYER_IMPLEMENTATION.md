# 音频播放器实现计划

## 概述
复刻 Voice-main 项目的音频播放功能，实现完整的音频播放详情页。

## 参考项目
- 项目路径：`~/Desktop/Voice-main`
- 核心模块：`features/playbackScreen`
- 播放核心：`core/playback`

## 需要实现的核心功能

### 1. 播放器界面 (BookPlayView)
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/view/BookPlayView.kt`

#### 界面组件：
- **顶部栏 (BookPlayAppBar)**
  - 关闭按钮
  - 睡眠定时器
  - 书签功能
  - 溢出菜单（播放速度、跳过静音、音量增强）

- **封面区域 (CoverRow)**
  - 显示音频封面/占位图
  - 播放/暂停按钮叠加
  - 睡眠定时器状态显示

- **章节信息 (ChapterRow)**
  - 当前章节名称
  - 上一章/下一章按钮
  - 点击显示章节列表

- **进度条 (SliderRow)**
  - 当前播放时间
  - 总时长
  - 可拖动的进度条

- **播放控制 (PlaybackRow)**
  - 快退按钮（使用设置中的跳过时间）
  - 播放/暂停按钮
  - 快进按钮（使用设置中的跳过时间）

### 2. 播放器核心功能

#### 2.1 MediaPlayer 集成
**参考文件**：`core/playback/src/main/kotlin/voice/core/playback/player/VoicePlayer.kt`

功能：
- 使用 Android MediaPlayer 或 ExoPlayer
- 播放/暂停控制
- 进度跟踪
- 播放速度调整
- 自动回退功能（暂停后恢复时回退N秒）

#### 2.2 播放状态管理
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/BookPlayViewModel.kt`

状态包括：
```kotlin
data class AudioPlayViewState(
    val audioId: Long,
    val title: String,
    val artist: String?,
    val cover: Uri?,
    val duration: Duration,
    val playedTime: Duration,
    val playing: Boolean,
    val chapterName: String?,
    val showPreviousNextButtons: Boolean,
    val sleepTimerState: SleepTimerViewState,
    val playbackSpeed: Float,
    val skipSilence: Boolean,
    val volumeBoost: Boolean
)
```

#### 2.3 播放控制器
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/BookPlayController.kt`

功能：
- `play()` - 播放
- `pause()` - 暂停
- `seekTo(position: Duration)` - 跳转到指定位置
- `rewind()` - 快退
- `fastForward()` - 快进
- `setSpeed(speed: Float)` - 设置播放速度
- `skipToNext()` - 下一章
- `skipToPrevious()` - 上一章

### 3. 附加功能

#### 3.1 睡眠定时器
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/view/BookPlayAppBar.kt`

功能：
- 设置倒计时（5分钟、10分钟、15分钟、30分钟、60分钟、章节结束）
- 显示剩余时间
- 取消定时器

#### 3.2 播放速度调整
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/SpeedDialog.kt`

功能：
- 速度范围：0.5x - 3.0x
- 使用滑块调整
- 显示当前速度

#### 3.3 章节选择
**参考文件**：`features/playbackScreen/src/main/kotlin/voice/features/playbackScreen/SelectChapterDialog.kt`

功能：
- 显示所有章节列表
- 当前章节高亮
- 点击跳转到指定章节

#### 3.4 书签功能
功能：
- 添加书签到当前位置
- 长按显示书签列表
- 跳转到书签位置

### 4. 实现步骤

#### 阶段1：基础播放器
1. 创建 `AudioPlayer` 类（封装 MediaPlayer）
2. 创建 `AudioPlayViewModel`（管理播放状态）
3. 创建基础播放界面（封面、播放/暂停、进度条）
4. 实现播放/暂停功能

#### 阶段2：播放控制
1. 实现快进/快退功能
2. 实现进度条拖动
3. 实现播放速度调整
4. 实现自动回退功能

#### 阶段3：高级功能
1. 实现睡眠定时器
2. 实现章节管理（如果音频有章节信息）
3. 实现书签功能
4. 实现跳过静音功能

#### 阶段4：UI优化
1. 添加动画效果
2. 支持横屏布局
3. 优化触摸反馈
4. 添加加载状态

### 5. 关键技术点

#### 5.1 MediaPlayer 生命周期管理
```kotlin
class AudioPlayer(context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    
    fun prepare(uri: Uri) {
        release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            prepareAsync()
            setOnPreparedListener { /* 准备完成 */ }
            setOnCompletionListener { /* 播放完成 */ }
            setOnErrorListener { /* 错误处理 */ }
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
```

#### 5.2 播放进度更新
```kotlin
LaunchedEffect(playing) {
    while (playing) {
        delay(100) // 每100ms更新一次
        currentPosition = player.getCurrentPosition()
    }
}
```

#### 5.3 播放速度调整
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
}
```

### 6. 文件结构

```
app/shared/src/
├── androidMain/kotlin/
│   ├── audio/
│   │   ├── AudioPlayer.kt              # MediaPlayer 封装
│   │   ├── AudioPlaybackController.kt  # 播放控制器
│   │   └── AudioPlaybackService.kt     # 后台播放服务
│   └── ui/
│       └── player/
│           ├── AudioPlayerScreen.kt    # 播放器主界面
│           ├── AudioPlayerViewModel.kt # ViewModel
│           ├── components/
│           │   ├── CoverSection.kt     # 封面区域
│           │   ├── ProgressSlider.kt   # 进度条
│           │   ├── PlaybackControls.kt # 播放控制按钮
│           │   └── PlayerAppBar.kt     # 顶部栏
│           └── dialogs/
│               ├── SpeedDialog.kt      # 速度调整对话框
│               └── SleepTimerDialog.kt # 睡眠定时器对话框
└── commonMain/kotlin/
    └── data/
        └── player/
            └── AudioPlayState.kt       # 播放状态数据类
```

## 注意事项

1. **权限管理**：确保有音频播放权限
2. **后台播放**：使用 Foreground Service
3. **通知栏控制**：添加媒体通知
4. **耳机控制**：支持耳机按钮控制
5. **电池优化**：处理电池优化限制
6. **状态保存**：保存播放位置，下次打开继续播放

## 测试要点

1. 播放/暂停功能
2. 进度条拖动准确性
3. 快进/快退功能
4. 播放速度调整
5. 睡眠定时器倒计时
6. 后台播放稳定性
7. 通知栏控制
8. 耳机按钮响应
9. 应用切换后状态保持
10. 内存泄漏检查

## 参考资源

- Voice项目：`~/Desktop/Voice-main`
- Android MediaPlayer文档：https://developer.android.com/guide/topics/media/mediaplayer
- ExoPlayer文档：https://exoplayer.dev/
- Media3文档：https://developer.android.com/media/media3
