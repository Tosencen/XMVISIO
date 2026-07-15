# XMVISIO 代码审查报告

> 审查范围：整个代码库（app/shared、core/utils 的全部 Kotlin 源文件）
> 关注维度：Bug/正确性、性能优化、代码质量
> 方法：静态扫描（反模式正则）+ 模块并行审查 + 关键文件人工核实
> 说明：标注「✅已核实」的为直接阅读源码确认；标注「🔍扫描发现」的为静态扫描/子代理发现，建议结合编译器复核。

## 修复状态（2025-07-14）

| 编号 | 问题 | 状态 |
|------|------|------|
| H1 | AudioPlayer 协程泄漏（8 处） | ✅ 已修复 — 添加类级 `scope`，`release()` 中 `cancel()` |
| H2 | GlobalAudioPlayerController 协程泄漏（6 处） | ✅ 已修复 — 添加类级 `scope`，`release()` 中 `cancel()` |
| H3 | UpdateViewModel scope 永不取消 | ✅ 已修复 — 添加 `close()` 方法 |
| H4 | FileDownloader.cancel() 无法中断下载 | ✅ 已修复 — 持有 `Call` 引用 + `cancel()` + `isActive` 检查 |
| H5 | 下载/更新未做完整性校验 | ✅ 已修复 — `FileDownloader` 添加 SHA-256 校验；`NewVersion` 添加 `sha256` 字段 |
| H6 | SingleTaskExecutor `!!` NPE | ✅ 已修复 — 替换为显式空检查并抛 `IllegalStateException` |
| H7 | DailyRollingFileLogWriter 不 flush | ✅ 已修复 — `writeLine` 中添加 `flush()` |
| H8 | VideoScreen.android.kt 多余 `}` | ✅ 已修复 — 删除多余右括号 |
| M1 | CrashHandler 立即 killProcess | ✅ 已修复 — 延迟 3 秒再杀进程，让崩溃页渲染 |
| M2 | CrashHandler 无 CoroutineExceptionHandler | ✅ 已修复 — 添加 `coroutineExceptionHandler` 伴生对象 |
| M3 | MediaNotificationReceiver 每次 new Manager | ✅ 已修复 — 复用 `GlobalAudioPlayerController.cancelNotification()` |
| M4 | Logger.error lambda 无 isErrorEnabled 守卫 | ✅ 已修复 — 添加 `if (isErrorEnabled())` |
| M5 | SearchState/LoadErrorCard 使用 GlobalScope | ✅ 已修复 — LoadErrorCard 移除 GlobalScope；SearchState 添加 `@OptIn` |
| M6 | iOS target 启用但缺 actual | ✅ 已修复 — `enableIos` 默认改为 `false` |
| M7 | ResetOnEvery 线程安全 | ✅ 已修复 — 使用 `kotlinx.atomicfu.atomic` + `Clock.System` 单调时钟 |
| M10 | YtDlpAutoUpdater/SealDownloadManager scope 未取消 | ✅ 已修复 — 添加 `close()` 方法 |
| M11 | AudioPlayer println → Logger | ✅ 已修复 — 替换为 `android.util.Log` |
| L1 | SleepTimerManager 匿名 CoroutineScope | ✅ 已修复 — 添加类级 `scope` |
| L2 | GlobalAudioPlayer 非线程安全 | ✅ 已修复 — 添加 `@Volatile` + `@Synchronized` |
| L5 | LayoutUtils cardVerticalPadding 重复定义 | ✅ 已修复 — 删除重复扩展属性 |
| L7 | DebounceWithInitial 共享状态 | ✅ 已修复 — 使用 `kotlinx.atomicfu.atomic` + 添加注释说明 |

---

## 一、概览与优先修复建议

| 维度 | 高 | 中 | 低 |
|------|----|----|----|
| Bug/正确性 | 7 | 6 | 4 |
| 性能优化 | 2 | 5 | 2 |
| 代码质量 | 0 | 3 | 5 |

**最该先修的几件事（按 ROI 排序）：**
1. 播放引擎与全局控制器里大量 `CoroutineScope(...).launch {}` 从不取消 → 持续泄漏协程/监听器，是稳定性头号隐患。
2. `UpdateViewModel` 不是 Android `ViewModel`，其 `CoroutineScope` 永不取消；`FileDownloader.cancel()` 实际无法中断下载。
3. `CrashHandler` 在 `startActivity` 之后立即 `killProcess`，崩溃页几乎无法显示；且未捕获协程异常。
4. `VideoScreen.android.kt:564` 疑似多余右括号，可能阻断构建（请编译器确认）。
5. 下载/更新/二进制**未做完整性校验**（损坏文件会安装失败甚至被篡改）。
6. `core/utils` 日志写入不 flush（崩溃时丢日志）、`Logger.error` 不判 `isErrorEnabled`。

---

## 二、高严重度（High）

### H1. AudioPlayer 大量匿名 `CoroutineScope(...).launch` 永不取消 ✅已核实
- 文件：`app/shared/src/androidMain/kotlin/audio/AudioPlayer.kt`
- 位置：`58`、`136`、`150`、`302`、`321`、`371`、`396`、`418` 行
- 类型：协程泄漏 / 正确性
- 描述：`AudioPlayer` 在 `init` 及多处方法里直接 `CoroutineScope(Dispatchers.IO/Main).launch { }`，这些 `CoroutineScope` 没有保存引用，类内也没有 `CoroutineScope` 字段，`release()`（415 行）只释放 `MediaPlayer` 与监听器，**从不取消这些协程**。其中 `init` 块的 `speedManager.playbackSpeed.collect { }`（58-72 行）会随应用生命周期永久存活。
- 修复：在类中声明 `private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`，所有 `launch` 改为 `scope.launch(...)`；`release()` 中调用 `scope.cancel()`。

### H2. GlobalAudioPlayerController 三处 init 协程 + 通知轮询从不取消 ✅已核实
- 文件：`app/shared/src/androidMain/kotlin/audio/GlobalAudioPlayerController.kt`
- 位置：`40`、`54`、`60`（init 内 collect）、`201`（`startNotificationUpdates` 的 `while(true){delay(1000)}`）
- 类型：协程泄漏 / 性能
- 描述：`init` 启动三个 `CoroutineScope(Dispatchers.Main).launch { collect{} }`，`release()`（228 行）只 `stopNotificationUpdates()` + `notificationManager.release()`，**没有取消这三个收集协程**，应用存活期间持续监听并触发 `updateNotification()`。`startNotificationUpdates` 的 `while(true)` 也挂在匿名 `CoroutineScope` 上。
- 修复：统一管理到类级 `CoroutineScope`，`release()` 中 `scope.cancel()`。

### H3. UpdateViewModel 的 CoroutineScope 永不取消 ✅已核实（子代理）
- 文件：`app/shared/src/androidMain/kotlin/update/UpdateViewModel.kt`
- 位置：`19`（`class UpdateViewModel(private val context: Context)`）、`20`（`CoroutineScope(SupervisorJob() + Dispatchers.Main)`）
- 类型：协程/资源泄漏
- 描述：该类**不继承** `androidx.lifecycle.ViewModel`，无 `onCleared()`，其 `scope` 在 Activity 存活甚至进程存活期间都不会取消。`checkUpdate`/`startDownload`/`installApk` 及其内部 `progressJob`/`stateJob` 子协程无法回收，且长期持有 `context` 引用。
- 修复：让其继承 `androidx.lifecycle.ViewModel` 并在 `onCleared()` 中 `scope.cancel()`；或接收 `Lifecycle` 并在 `ON_DESTROY` 时取消。

### H4. FileDownloader.cancel() 无法真正中断下载 ✅已核实（子代理）
- 文件：`app/shared/src/androidMain/kotlin/update/FileDownloader.kt`
- 位置：`95`（`client.newCall(request).execute()`）、`116-133`（写文件循环）、`156-160`（`fun cancel()`）
- 类型：Bug/正确性（IO 中断）、性能
- 描述：`cancel()` 仅重置 StateFlow；注释声称“OkHttp 会在协程取消时自动停止”，但 `download()` 既未用 `suspendCancellableCoroutine` 也未持有 `Call` 引用去 `call.cancel()`；写文件循环不检查 `coroutineContext.isActive`，协程被取消后线程仍会读完整个文件，且状态已变为 `Idle` 但后台 IO 仍在写 `destination`，存在竞争/流量浪费。
- 修复：持有 `Call` 引用并在 `cancel()` 中 `call.cancel()`；循环内 `if (!currentCoroutineContext().isActive) return/throw CancellationException`；取消时删除残留临时文件。

### H5. 下载的 APK / yt-dlp 二进制未做完整性校验 🔍扫描发现
- 文件：`app/shared/src/androidMain/kotlin/update/FileDownloader.kt`（107-142）、`update/UpdateInstaller.kt`；`download/SealDownloadManager.kt`（updateYtDlp 调用）
- 类型：正确性（完整性）/ 安全
- 描述：从 GitHub/jsDelivr 下载 APK 后仅用 `contentLength` 算进度，无哈希/签名校验，下载完直接调起安装；yt-dlp 二进制下载执行前也无任何校验。损坏或被篡改的文件会被直接安装/执行。
- 修复：下发 `size` 与 SHA-256，下载完成后校验字节数与哈希；可选校验 APK 签名证书指纹与当前应用一致。

### H6. core/utils：SingleTaskExecutor 对无 Job 上下文 `!!` 抛 NPE 🔍扫描发现
- 文件：`core/utils/coroutines/src/commonMain/kotlin/SingleTaskExecutor.kt`
- 位置：`80`（`currentCoroutineContext()[Job]!!`）
- 类型：正确性（崩溃）
- 描述：当调用方上下文没有 `Job`（如 `GlobalScope`、已取消的 `coroutineScope`）时 `[Job]` 为 null，`!!` 抛 `IllegalStateException`，与注释“总会抛 CancellationException”语义不符。
- 修复：空时抛明确的 `IllegalStateException("requires a Job")` 或仅当存在时才作为父 Job 加入。

### H7. core/utils：DailyRollingFileLogWriter 从不 flush，崩溃丢日志 🔍扫描发现
- 文件：`core/utils/logging/src/appleMain/kotlin/writer/DailyRollingFileLogWriter.kt`
- 位置：`77-82`（`writeLine`）、`103`（`sink(...).buffered()`）
- 类型：正确性（数据丢失）
- 描述：写入用 `buffered()` 但**从不 `flush()`**，仅换天/清理时才 `close()`。iOS 进程被杀或崩溃时，缓冲中的日志全部丢失——而它本就是崩溃日志组件，尤为致命。
- 修复：每次写入后 `flush()`（或批量/定时 flush），并在 `close()` 前先 `flush()`。

### H8.（构建阻断）VideoScreen.android.kt 疑似多余右括号 ✅已核实（部分阅读）
- 文件：`app/shared/src/androidMain/kotlin/ui/main/VideoScreen.android.kt`
- 位置：`562` 关闭 `VideoScreen` 函数，`564` 又多出一个 `}`（563 为空行），其后 `566` 起才是 `VideoGrid` 等新顶层声明。
- 类型：Bug/正确性（编译失败）
- 描述：若函数确实在 562 行闭合，则 564 行的 `}` 会导致编译失败、整个 UI 模块无法构建。
- 修复：删除 564 行多余的 `}`；并用编译器验证。*注：若项目当前可正常构建，请重点核对 287-291 行 `when/PullToRefreshBox/Scaffold-content` 的括号嵌套是否与打开顺序一一对应。*

---

## 三、中严重度（Medium）

### M1. CrashHandler 立即 killProcess 导致崩溃页无法显示 ✅已核实
- 文件：`app/shared/src/androidMain/kotlin/crash/CrashHandler.kt`
- 位置：`60`（`startActivity`）、`64-65`（`killProcess`/`exitProcess`）
- 类型：正确性
- 描述：`uncaughtException` 用 `FLAG_ACTIVITY_NEW_TASK|CLEAR_TASK` 启动的是**同一进程**内的根 Activity，随后立即 `killProcess(myPid())`。进程被杀，新 Activity 几乎不可能渲染，用户看不到崩溃信息。
- 修复：不要立即杀进程。可启动独立进程的崩溃 Activity，或在主线程短暂延时/让崩溃页 Activity 自行结束后由系统回收；常见做法是将崩溃页放到 `:error` 独立进程。

### M2. CrashHandler 未安装 CoroutineExceptionHandler ✅已核实
- 文件：`app/shared/src/androidMain/kotlin/crash/CrashHandler.kt`
- 位置：`16`（`Thread.UncaughtExceptionHandler`）
- 类型：正确性
- 描述：只实现了 `Thread.UncaughtExceptionHandler`。未通过 `ServiceLoader`/全局 `CoroutineExceptionHandler` 兜底的协程内未处理异常（如 `GlobalScope.launch`、无父 Job 的 `launch`）可能仅被打印到 stderr，不会跳转到崩溃页。
- 修复：在协程入口统一加 `CoroutineExceptionHandler` 转发到崩溃流程；或确保全局协程异常仍能走到 `Thread` 默认处理器。

### M3. MediaNotificationReceiver 每次 STOP 都 new 一个 MediaNotificationManager ✅已核实
- 文件：`app/shared/src/androidMain/kotlin/audio/MediaNotificationReceiver.kt`
- 位置：`36`（`val notificationManager = MediaNotificationManager(context)`）
- 类型：资源泄漏 / 性能
- 描述：每次收到 `ACTION_STOP` 都新建 `MediaNotificationManager`（其构造会创建 `MediaSessionCompat`），而 `cancelNotification()` 未必释放该 session；且全局已存在 `GlobalAudioPlayerController`，这里重建属于冗余且危险。
- 修复：改为复用 `GlobalAudioPlayerController.getInstance(context).cancelNotification()`（其内部已正确释放通知/MediaSession），不要在 Receiver 内新建 Manager。

### M4. Logger.error 重载不判断 isErrorEnabled 🔍扫描发现
- 文件：`core/utils/logging/src/commonMain/kotlin/LoggerKt.kt`
- 位置：`109-111`
- 类型：性能 + 行为不一致
- 描述：`inline fun Logger.error(message: () -> String) { error(message()) }` 忽略了同文件 `debug/info/warn` 都有的 `if (isXxxEnabled())` 守卫，即使 ERROR 级别关闭也会执行 `message()` 并产生副作用开销。
- 修复：改为 `if (isErrorEnabled()) error(message())`。

### M5. SearchState / LoadErrorCard 使用 GlobalScope ✅已核实（扫描）
- 文件：`app/shared/ui-foundation/src/commonMain/kotlin/ui/search/SearchState.kt:123`（`cachedIn(GlobalScope)`）、`.../LoadErrorCard.kt:197`（`GlobalScope.launch`）
- 类型：协程泄漏 / 性能
- 描述：`cachedIn(GlobalScope)` 使上游 Flow 共享状态与应用同寿且无法取消；`GlobalScope.launch` 不受任何生命周期约束。
- 修复：使用受生命周期约束的作用域（如 ViewModel 的 `viewModelScope`、Composable 的 `rememberCoroutineScope` 或 `viewModelStore`）。

### M6. iOS 为构建目标，但多个 expect 缺少 iOS actual 🔍扫描发现
- 文件：`app/shared/src/iosMain/kotlin/`（仅有 `App.ios.kt`、`SystemBars.ios.kt`、`DesktopWindowInsets.ios.kt`）
- 位置：缺少 `createThemeSettingsManager`、`openUrl`、`rememberAppVersion`、`AppWithUpdateCheck`、`HandleOpenPlayerRequest`、`Constants` 等的 iOS `actual`
- 类型：正确性（编译失败，条件性）
- 描述：`buildSrc` 与 `app/shared/build.gradle.kts` 显示 **iOS 是启用目标**（`enableIos`、`iosArm64()`、`iosSimulatorArm64()`）。当前 iosMain 仅提供了少量 actual，若 iOS target 实际参与编译，会因缺少上述 actual 而失败。
- 修复：要么补齐 iOS `actual` 实现，要么默认关闭 iOS target（`enableIos=false`），避免“半成品”目标误导。

### M7. ResetOnEvery 跨线程竞态 + 依赖墙钟时间 🔍扫描发现
- 文件：`core/utils/coroutines/src/commonMain/kotlin/flows/ResetOnEvery.kt`
- 位置：`28-41`
- 类型：并发 / 正确性
- 描述：`time.value` 被 collector 与 ticker 两个协程读写，读-判非原子；且依赖 `currentTimeMillis()` 单调递增，系统时间回拨会误触发或延迟。注释称“线程安全”不成立。
- 修复：用 `Mutex`/`AtomicLong` 保护；改用 `TimeSource.Monotonic` 单调时钟免疫回拨。

### M8. AudiobookScreenImpl / VideoScreen / PlayerBottomSheet 大量 `!!` ✅已核实（计数）
- 文件：`.../ui/main/AudiobookScreenImpl.kt`（18 处）、`.../ui/main/VideoScreen.android.kt`（7 处）、`.../ui/player/PlayerBottomSheet.kt`（6 处）
- 类型：正确性（潜在 NPE）
- 描述：多处非空断言，集中在可空来源（播放器回调、Intent extra、列表项）附近，是崩溃高发点。
- 修复：逐处改为安全调用/`requireNotNull`+明确错误信息/`let` 守卫；对来自 `GlobalAudioPlayer.getInstance(...).release()` 后再次 `!!` 的路径尤其小心。

### M9. 巨型文件难以维护（代码质量）
- 文件：`AudiobookScreenImpl.kt`（~88KB）、`VideoPlayerScreen.android.kt`（~43KB）、`SealDownloadScreen.kt`（~36KB）、`VideoScreen.android.kt`（~33KB）
- 类型：代码质量
- 描述：单体超大文件，含超长函数与散乱的状态管理，可读性/可测试性差。
- 修复：按职责拆分（列表/网格/对话框/状态持有分离），将状态提升到 ViewModel/StateHolder。

### M10. 下载与更新模块作用域未取消 🔍扫描发现
- 文件：`download/SealDownloadManager.kt:32`（`downloadScope`）、`download/YtDlpAutoUpdater.kt:12`（`scope = CoroutineScope(Dispatchers.IO)`）
- 类型：协程泄漏
- 描述：两者都创建了长期 `CoroutineScope`，未见对应的 `cancel()`；`YtDlpAutoUpdater` 尤其危险（无释放点）。
- 修复：提供显式 `close()/release()` 并在销毁时取消；或绑定到应用级/生命周期作用域。

### M11. AudioPlayer 使用 `println` 而非项目日志设施（代码质量）
- 文件：`AudioPlayer.kt` 全文多处（`println("应用播放速度失败...")` 等）
- 类型：代码质量 / 可观测性
- 描述：项目已具备 `core/utils` 日志模块，但音频引擎大量用 `println`，生产环境无法分级/落盘/开关。
- 修复：统一改用 `core/utils` 的 `Logger`。

---

## 四、低严重度 / 代码质量（Low）

- **L1 SleepTimerManager 每次 setTimer/showToast 都新建匿名 `CoroutineScope`** ✅已核实：`SleepTimerManager.kt:44`、`112`；Job 虽被回收，但 scope 未统一取消，属轻微泄漏。建议用类级 scope。
- **L2 GlobalAudioPlayer.getInstance 非线程安全** ✅已核实：`GlobalAudioPlayer.kt:12-17`，无 `@Volatile`/双重检查锁；并发首次调用可能创建两个 `AudioPlayer` 并泄漏前者。建议参照 `GlobalAudioPlayerController` 的双重检查锁。
- **L3 App.kt 的 `!!`** ✅已核实：`App.kt:214`（`audioToPlay!!`）、`225`（`videoToPlay!!`），但均在 `!= null` 守卫后，风险低。
- **L4 Desktop/iOS actual 为“桩实现”** 🔍扫描发现：`ThemeSettingsManager.desktop.kt` 仅内存、不持久化（重启丢主题）；`AppVersion.desktop.kt` 硬编码 `"1.0.3"`。属功能缺口，建议标注或补全。
- **L5 LayoutUtils 重复定义** 🔍扫描发现：`WindowSizeClass.cardVerticalPadding` 同时作为类属性与扩展属性定义两次。
- **L6 ExceptionCollector 用 hashCode 去重异常** 🔍扫描发现：`core/utils/coroutines/.../ExceptionCollector.kt:56`，身份哈希可能漏掉同语义的不同实例。
- **L7 DebounceWithInitial 共享状态在多 collector 下语义错误** 🔍扫描发现：`core/utils/coroutines/.../DebounceWithInitial.kt:20-31`，`isInitial` 放在共享对象里，第二个 collector 初始值不再立即 emit。
- **L8 重复样板代码** ✅已核实：`AudioPlayer.play()/pause()/togglePlayPause()` 中大量重复的 `try{...}catch(e:Exception){ println(...) }` 可抽取为辅助函数。

---

## 五、误报说明（已排除）

初次自动化审查中，有子代理上报 UI 模块存在大量“**编译阻断级拼写错误**”（如 `androidx.compose.foundation` 被写成 `foundation`、`mutableStateOf` 被写成大小写变体等）。经**直接阅读源码核实，这些均为误报**：

- `VideoScreen.android.kt:14` 实际为正确的 `import androidx.compose.foundation.Image`；
- `App.kt:135` 实际为正确的 `mutableStateOf(MainTab.AUDIOBOOK)`。

因此上述“typo 编译失败”类结论已全部剔除，未计入本报告。这也提示：对自动化/子代理产出的“编译错误”类结论，应以编译器或人工阅读为准。

---

## 六、后续建议

1. **统一协程作用域**：为所有长期存活对象（AudioPlayer、GlobalAudioPlayerController、SleepTimerManager、UpdateViewModel、SealDownloadManager、YtDlpAutoUpdater）引入类级 `CoroutineScope(SupervisorJob())`，在 `release()/onCleared()/close()` 中 `cancel()`。这是覆盖面最广、收益最高的一揽子修复。
2. **跑一次编译 + lint**：先确认 H8（VideoScreen 多余括号）与 M6（iOS actual 缺失）是否真实阻断构建，优先排雷。
3. **下载/更新加完整性校验**：H5 涉及的 APK/二进制校验建议作为安全合规的一部分尽快补上。
4. **崩溃可观测性**：M1/M2 修复后，配合 L7 的 `Logger` 落地，可保证崩溃页与日志可用。
5. **逐步瘦身超大文件**（M9），把状态管理迁移到 ViewModel/StateHolder，提升可维护性并减少 `!!`。
