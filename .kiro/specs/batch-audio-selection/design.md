# 设计文档

## 概述

批量音频选择和分类功能允许用户在音频列表页面快速选择多个音频文件，并通过底部操作面板将它们批量添加到指定分类中。该功能通过在现有的 `AudiobookScreenImpl` 组件中添加批量选择模式来实现，保持与现有UI风格的一致性。

## 架构

### 组件层次结构

```
AudiobookScreenImpl (修改)
├── TopAppBar (修改)
│   ├── 全选/取消按钮 (新增)
│   ├── 搜索按钮
│   └── 菜单按钮
├── 分类标签行 (保持不变)
├── HorizontalPager (修改)
│   └── AudioListWithSelection (修改)
│       └── AudioCardWithCheckbox (修改)
└── BatchSelectionBottomPanel (新增)
    ├── 关闭按钮
    ├── 选中计数
    └── 分类列表
```

### 状态管理

批量选择功能的状态将在 `AudiobookScreenImpl` 组件中管理：

- `isBatchSelectionMode: Boolean` - 是否处于批量选择模式
- `selectedAudioIds: Set<Long>` - 当前选中的音频ID集合
- `showBatchPanel: Boolean` - 是否显示底部操作面板（派生状态：selectedAudioIds.isNotEmpty()）

## 组件和接口

### 1. BatchSelectionState (新增)

状态管理类，封装批量选择相关的状态和操作。

```kotlin
data class BatchSelectionState(
    val isActive: Boolean = false,
    val selectedAudioIds: Set<Long> = emptySet()
) {
    val hasSelection: Boolean get() = selectedAudioIds.isNotEmpty()
    val selectionCount: Int get() = selectedAudioIds.size
    
    fun toggleSelection(audioId: Long): BatchSelectionState {
        return if (audioId in selectedAudioIds) {
            copy(selectedAudioIds = selectedAudioIds - audioId)
        } else {
            copy(selectedAudioIds = selectedAudioIds + audioId)
        }
    }
    
    fun selectAll(audioIds: List<Long>): BatchSelectionState {
        return copy(selectedAudioIds = audioIds.toSet())
    }
    
    fun clearSelection(): BatchSelectionState {
        return copy(selectedAudioIds = emptySet())
    }
    
    fun exitMode(): BatchSelectionState {
        return BatchSelectionState()
    }
}
```

### 2. BatchSelectionBottomPanel (新增)

底部操作面板组件，显示分类列表和操作按钮。

```kotlin
@Composable
fun BatchSelectionBottomPanel(
    selectedCount: Int,
    categories: List<AudioCategory>,
    onCategoryClick: (AudioCategory) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)
```

**参数说明：**
- `selectedCount`: 当前选中的音频数量
- `categories`: 可用的分类列表（不包括"全部"）
- `onCategoryClick`: 点击分类时的回调
- `onClose`: 点击关闭按钮的回调

**UI布局：**
- 使用 `ModalBottomSheet` 或自定义底部面板
- 顶部显示关闭按钮和选中计数
- 中间显示分类列表（横向滚动或网格布局）
- 使用 `AnimatedVisibility` 实现平滑的显示/隐藏动画

### 3. AudioCardWithCheckbox (修改)

修改现有的音频卡片组件，添加复选框支持。

```kotlin
@Composable
fun AudioCardWithCheckbox(
    audio: LocalAudioFile,
    isSelected: Boolean,
    isBatchMode: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**修改内容：**
- 在卡片左侧添加 `Checkbox`，仅在 `isBatchMode` 为 true 时显示
- 在批量选择模式下，点击卡片触发选择而非播放
- 在批量选择模式下，禁用长按菜单
- 选中状态下添加背景色高亮

### 4. CategoryManager 扩展 (修改)

添加批量设置音频分类的方法。

```kotlin
suspend fun setAudioCategoryBatch(
    audioIds: List<Long>,
    categoryId: String?
): Result<Unit>
```

## 数据模型

### BatchSelectionState

```kotlin
data class BatchSelectionState(
    val isActive: Boolean,
    val selectedAudioIds: Set<Long>
)
```

### 现有模型（无需修改）

- `LocalAudioFile`: 音频文件信息
- `AudioCategory`: 分类信息
- `AudioCategoryMapping`: 音频与分类的映射关系

## 正确性属性

*属性是应该在系统所有有效执行中保持为真的特征或行为——本质上是关于系统应该做什么的正式陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1: 选择状态切换一致性

*对于任何* 音频ID和批量选择状态，当切换该音频的选择状态时，该音频ID应该在选中集合中当且仅当它之前不在选中集合中。

**验证: 需求 2.2**

### 属性 2: 全选完整性

*对于任何* 音频列表，当执行全选操作时，选中集合应该包含列表中所有音频的ID。

**验证: 需求 2.5**

### 属性 3: 面板显示条件

*对于任何* 批量选择状态，底部操作面板应该显示当且仅当选中集合非空。

**验证: 需求 3.1, 3.2**

### 属性 4: 选中计数准确性

*对于任何* 批量选择状态，显示的选中数量应该等于选中集合的大小。

**验证: 需求 2.3, 2.4, 6.2**

### 属性 5: 批量分类完整性

*对于任何* 选中的音频ID列表和目标分类，当执行批量分类操作后，所有选中的音频都应该属于该目标分类。

**验证: 需求 4.2**

### 属性 6: 模式退出清理

*对于任何* 批量选择状态，当退出批量选择模式时，选中集合应该被清空且批量模式标志应该为false。

**验证: 需求 1.4, 3.4, 5.4**

### 属性 7: 分类切换清理

*对于任何* 批量选择状态和分类切换操作，当用户切换到不同分类标签时，选中集合应该被清空。

**验证: 需求 5.3**

### 属性 8: 模式激活时的UI状态

*对于任何* 批量选择状态，当批量模式激活时，所有音频卡片应该显示复选框且点击行为应该触发选择而非播放。

**验证: 需求 2.1, 6.3**

### 属性 9: 分类列表过滤

*对于任何* 分类集合，操作面板中显示的分类列表应该不包含"全部"分类。

**验证: 需求 4.1**

## 错误处理

### 1. 批量分类失败

**场景**: 批量添加音频到分类时发生错误

**处理策略**:
- 使用事务性操作，确保要么全部成功，要么全部回滚
- 失败时显示 Snackbar 错误提示
- 保持批量选择模式，允许用户重试

### 2. 空分类列表

**场景**: 用户尚未创建任何自定义分类

**处理策略**:
- 在底部面板显示"暂无分类"提示
- 提供"创建新分类"快捷按钮
- 引导用户先创建分类

### 3. 分类切换时的状态冲突

**场景**: 用户在批量选择模式下切换分类标签

**处理策略**:
- 自动清除选中状态
- 保持批量选择模式激活
- 不显示底部面板（因为选中集合为空）

## 测试策略

### 单元测试

1. **BatchSelectionState 状态转换测试**
   - 测试 `toggleSelection` 正确添加/移除ID
   - 测试 `selectAll` 正确设置所有ID
   - 测试 `clearSelection` 清空选中集合
   - 测试 `exitMode` 重置所有状态

2. **CategoryManager 批量操作测试**
   - 测试批量设置分类成功场景
   - 测试批量设置分类失败回滚
   - 测试空列表处理

### 集成测试

1. **批量选择流程测试**
   - 进入批量选择模式
   - 选择多个音频
   - 验证底部面板显示
   - 执行批量分类
   - 验证退出模式

2. **分类切换测试**
   - 在批量选择模式下切换分类
   - 验证选中状态被清除
   - 验证面板隐藏

3. **返回键处理测试**
   - 在批量选择模式下按返回键
   - 验证退出模式
   - 验证状态清理

### UI测试

1. **视觉反馈测试**
   - 验证复选框显示/隐藏
   - 验证选中状态高亮
   - 验证面板动画

2. **交互测试**
   - 验证卡片点击行为切换
   - 验证长按菜单禁用
   - 验证滚动功能保持

## 实现注意事项

### 1. 性能优化

- 使用 `Set<Long>` 存储选中ID，确保 O(1) 查找性能
- 使用 `remember` 和 `derivedStateOf` 避免不必要的重组
- 批量操作使用协程，避免阻塞UI线程

### 2. 动画和过渡

- 底部面板使用 `AnimatedVisibility` 实现平滑进入/退出
- 复选框使用 `AnimatedVisibility` 实现淡入/淡出
- 选中状态使用 `animateColorAsState` 实现背景色过渡

### 3. 状态持久化

- 批量选择状态不需要持久化（临时状态）
- 分类映射关系通过 `CategoryManager` 持久化到 SharedPreferences

### 4. 可访问性

- 为复选框添加内容描述
- 为批量操作按钮添加语义标签
- 确保触摸目标大小符合最小尺寸要求（48dp）

### 5. 与现有功能的集成

- 在批量选择模式下禁用播放功能
- 在批量选择模式下禁用长按菜单
- 在批量选择模式下禁用排序模式
- 保持搜索功能可用（搜索时退出批量选择模式）
