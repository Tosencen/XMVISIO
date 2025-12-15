# 实现计划 - 多功能媒体播放应用

## 第一阶段：框架调整和基础设施

- [ ] 1. 调整导航结构
  - 修改 `App.kt` 中的 `MainTab` 枚举，改为三个 Tab：有声书、视频、下载
  - 更新 `MainScreen` 的导航逻辑
  - 在下载页面右上角添加设置按钮
  - _需求：6.1, 6.2, 6.3, 6.4_

- [ ] 2. 创建基础页面框架
  - 创建 `ui/main/AudiobookScreen.kt` 空页面
  - 创建 `ui/main/VideoScreen.kt` 空页面
  - 创建 `ui/main/DownloadsScreen.kt` 空页面，包含右上角设置按钮
  - _需求：6.1, 6.2, 6.3_

- [ ] 3. 建立数据库基础设施
  - 添加 Room 依赖到 `build.gradle.kts`
  - 创建 `data/local/AppDatabase.kt` 数据库类
  - 创建数据库初始化和迁移逻辑
  - _需求：7.1, 7.2_

- [ ] 4. 建立 DataStore 配置存储
  - 添加 DataStore 依赖到 `build.gradle.kts`
  - 创建 `data/settings/SettingsDataStore.kt` 用于保存全局设置
  - 实现设置的序列化和反序列化
  - _需求：4.5, 4.6_

## 第二阶段：有声书功能

- [ ] 5. 创建有声书数据模型和数据层
  - 创建 `domain/audiobook/models/Audiobook.kt` 和 `AudiobookPlaybackState.kt`
  - 创建 `data/audiobook/local/AudiobookEntity.kt` 和 `AudiobookDao.kt`
  - 创建 `data/audiobook/AudiobookRepositoryImpl.kt`
  - _需求：1.1, 1.5, 7.1_

- [ ] 6. 实现有声书列表和文件扫描
  - 创建 `domain/audiobook/usecases/GetAudiobooksUseCase.kt`
  - 实现本地文件系统扫描逻辑
  - 创建 `ui/components/MediaList.kt` 通用列表组件
  - _需求：1.1_

- [ ] 7. 实现有声书播放器
  - 添加 ExoPlayer 依赖到 `build.gradle.kts`
  - 创建 `domain/audiobook/models/AudioPlayer.kt` 播放器接口
  - 创建 `data/audiobook/AudioPlayerImpl.kt` 实现
  - 创建 `ui/components/player/AudioPlayerControls.kt` UI 组件
  - _需求：1.2, 1.3, 1.4_

- [ ] 8. 实现有声书播放进度保存
  - 创建 `domain/audiobook/usecases/SavePlaybackProgressUseCase.kt`
  - 实现定期保存播放进度到数据库
  - 实现应用启动时恢复播放进度
  - _需求：1.5, 7.1_

- [ ] 9. 实现有声书菜单
  - 创建 `ui/settings/audiobook/AudiobookMenuDialog.kt`
  - 实现播放列表管理、书签功能、音质设置
  - _需求：5.1, 5.4_

- [ ]* 10. 编写有声书属性测试
  - **属性 1：播放进度持久化**
  - **验证需求：1.5, 7.1**

## 第三阶段：视频播放功能

- [ ] 11. 创建视频数据模型和数据层
  - 创建 `domain/video/models/Video.kt` 和 `VideoPlaybackState.kt`
  - 创建 `data/video/local/VideoEntity.kt` 和 `VideoDao.kt`
  - 创建 `data/video/VideoRepositoryImpl.kt`
  - _需求：2.1, 2.5, 7.1_

- [ ] 12. 实现视频列表和文件扫描
  - 创建 `domain/video/usecases/GetVideosUseCase.kt`
  - 实现本地视频文件扫描逻辑
  - 复用 `ui/components/MediaList.kt` 组件
  - _需求：2.1_

- [ ] 13. 实现视频播放器
  - 创建 `domain/video/models/VideoPlayer.kt` 播放器接口
  - 创建 `data/video/VideoPlayerImpl.kt` 实现
  - 创建 `ui/components/player/VideoPlayerControls.kt` UI 组件
  - 支持全屏切换和清晰度选择
  - _需求：2.2, 2.3, 2.4, 2.6_

- [ ] 14. 实现视频播放进度保存
  - 创建 `domain/video/usecases/SavePlaybackProgressUseCase.kt`
  - 实现定期保存播放进度到数据库
  - 实现应用启动时恢复播放进度
  - _需求：2.5, 7.1_

- [ ] 15. 实现视频菜单
  - 创建 `ui/settings/video/VideoMenuDialog.kt`
  - 实现播放列表管理、字幕设置、清晰度选择
  - _需求：5.2, 5.4_

- [ ]* 16. 编写视频属性测试
  - **属性 1：播放进度持久化**
  - **验证需求：2.5, 7.1**

## 第四阶段：下载管理功能

- [ ] 17. 创建下载数据模型和数据层
  - 创建 `domain/download/models/DownloadTask.kt` 和 `DownloadState.kt`
  - 创建 `data/download/local/DownloadEntity.kt` 和 `DownloadDao.kt`
  - 创建 `data/download/DownloadRepositoryImpl.kt`
  - _需求：3.1, 3.6, 7.2_

- [ ] 18. 实现下载管理器
  - 创建 `domain/download/models/DownloadManager.kt` 接口
  - 创建 `data/download/DownloadManagerImpl.kt` 实现
  - 支持创建、暂停、恢复、取消下载任务
  - _需求：3.1, 3.3, 3.4, 3.5_

- [ ] 19. 实现下载列表 UI
  - 创建 `ui/components/download/DownloadItem.kt` 组件
  - 创建 `ui/components/download/DownloadProgressBar.kt` 组件
  - 在 `DownloadsScreen` 中显示下载列表
  - _需求：3.2_

- [ ] 20. 实现下载进度显示
  - 实现实时进度更新
  - 显示下载速度和剩余时间
  - _需求：3.2_

- [ ] 21. 实现下载菜单
  - 创建 `ui/settings/downloads/DownloadsMenuDialog.kt`
  - 实现下载设置、存储位置选择、缓存清理
  - _需求：5.3, 5.4_

- [ ]* 22. 编写下载属性测试
  - **属性 2：下载任务状态一致性**
  - **验证需求：3.5, 3.6, 7.2**

- [ ]* 23. 编写下载暂停恢复属性测试
  - **属性 6：下载暂停恢复一致性**
  - **验证需求：3.3, 3.4**

## 第五阶段：主题和设置

- [ ] 24. 扩展主题设置功能
  - 在 `data/ThemeSettings.kt` 中添加语言设置
  - 创建 `domain/settings/models/AppSettings.kt`
  - 创建 `domain/settings/usecases/UpdateSettingsUseCase.kt`
  - _需求：4.1, 4.5, 4.6_

- [ ] 25. 实现多语言支持
  - 创建多语言资源文件
  - 实现语言切换逻辑
  - 在 `ThemeSettingsPage` 中添加语言选择
  - _需求：4.4_

- [ ] 26. 完善设置页面
  - 更新 `ui/settings/ThemeSettingsPage.kt`
  - 添加语言选择选项
  - 添加关于应用信息
  - _需求：4.1_

- [ ]* 27. 编写主题设置属性测试
  - **属性 3：主题设置立即生效**
  - **验证需求：4.2, 4.3, 4.4**

- [ ]* 28. 编写设置持久化属性测试
  - **属性 4：设置持久化和恢复**
  - **验证需求：4.5, 4.6**

## 第六阶段：集成和优化

- [ ] 29. 检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请提出。

- [ ] 30. 实现导航状态保持
  - 实现页面滚动位置保持
  - 实现列表状态保持
  - _需求：6.5_

- [ ]* 31. 编写导航属性测试
  - **属性 5：导航状态保持**
  - **验证需求：6.2, 6.5**

- [ ]* 32. 编写播放列表完整性属性测试
  - **属性 7：播放列表完整性**
  - **验证需求：7.3, 7.4**

- [ ] 33. 集成测试
  - 完整的播放流程测试
  - 完整的下载流程测试
  - 设置修改和应用流程测试
  - 导航和页面切换流程测试
  - _需求：1.1-1.5, 2.1-2.6, 3.1-3.6, 4.1-4.6, 6.1-6.5_

- [ ] 34. 性能优化
  - 优化列表滚动性能
  - 优化播放器内存使用
  - 优化数据库查询
  - _需求：所有_

- [ ] 35. 最终检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请提出。
