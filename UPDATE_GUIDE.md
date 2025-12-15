# XMVISIO 应用内更新指南

## 更新机制说明

XMVISIO 使用 GitHub Releases API 实现应用内自动更新功能。

### 工作原理

1. **检查更新**：应用通过 GitHub API 检查最新的 Release
2. **版本比较**：比较当前版本和最新版本号
3. **下载 APK**：使用 jsDelivr CDN 加速下载（自动回退到 GitHub）
4. **安装更新**：下载完成后提示用户安装

### 是否需要 GitHub Token？

**不需要！** 但有 Token 可以提高限制：

- **无 Token**：60 次/小时（足够个人使用）
- **有 Token**：5000 次/小时（推荐大量用户的应用）

### 如何配置 GitHub Token（可选）

1. 在 GitHub 创建 Personal Access Token：
   - 访问：https://github.com/settings/tokens
   - 点击 "Generate new token (classic)"
   - 权限选择：`public_repo`（只读公开仓库）
   - 生成并复制 Token

2. 在项目中配置（两种方式）：

   **方式一：本地配置（推荐）**
   ```properties
   # local.properties
   GITHUB_TOKEN=ghp_your_token_here
   ```

   **方式二：环境变量**
   ```bash
   export GITHUB_TOKEN=ghp_your_token_here
   ```

3. 在 `app/android/build.gradle.kts` 中读取：
   ```kotlin
   android {
       defaultConfig {
           // 从 local.properties 读取
           val properties = Properties()
           val localPropertiesFile = rootProject.file("local.properties")
           if (localPropertiesFile.exists()) {
               properties.load(localPropertiesFile.inputStream())
           }
           
           buildConfigField(
               "String",
               "GITHUB_TOKEN",
               "\"${properties.getProperty("GITHUB_TOKEN", "")}\""
           )
       }
       
       buildFeatures {
           buildConfig = true
       }
   }
   ```

### 发布新版本

1. **打包 APK**：
   ```bash
   ./gradlew :app:android:assembleRelease
   ```

2. **创建 GitHub Release**：
   - 访问：https://github.com/Tosencen/XMVISIO/releases/new
   - Tag version：`v1.0.1`（必须以 v 开头）
   - Release title：`XMVISIO v1.0.1`
   - 描述更新内容（支持 Markdown）
   - 上传 APK 文件
   - 点击 "Publish release"

3. **用户更新**：
   - 用户打开应用
   - 进入设置 → 软件更新
   - 点击检查更新
   - 发现新版本后下载并安装

### 更新内容格式（Markdown）

```markdown
## 更新内容

### 新功能
- 新增功能 A
- 新增功能 B

### 优化
- 优化了性能
- 改进了 UI

### 修复
- 修复了若干问题
```

### 注意事项

1. **版本号格式**：必须使用语义化版本号（如 1.0.0）
2. **Tag 格式**：必须以 `v` 开头（如 v1.0.0）
3. **APK 文件名**：必须以 `.apk` 结尾
4. **网络权限**：确保应用有网络和存储权限
5. **安装权限**：Android 8.0+ 需要"安装未知应用"权限

### API 限制

GitHub API 限制（无 Token）：
- 60 次/小时/IP
- 建议应用内设置检查间隔（如 24 小时检查一次）

### CDN 加速

应用使用 jsDelivr CDN 加速下载：
- 主 URL：`https://cdn.jsdelivr.net/gh/Tosencen/XMVISIO@v1.0.0/app-release.apk`
- 回退 URL：`https://github.com/Tosencen/XMVISIO/releases/download/v1.0.0/app-release.apk`

如果 CDN 下载失败，会自动切换到 GitHub 直接下载。

### 测试更新功能

1. 修改当前版本号为较低版本（如 0.9.0）
2. 在 GitHub 创建一个测试 Release（如 v1.0.0）
3. 在应用中点击检查更新
4. 验证下载和安装流程

## 完整实现文件

已创建的文件：
- `app/android/src/main/kotlin/com/xmvisio/app/Constants.kt` - 常量配置
- `app/android/src/main/kotlin/com/xmvisio/app/update/UpdateChecker.kt` - 更新检查器
- 待创建：
  - `UpdateViewModel.kt` - 更新状态管理
  - `FileDownloader.kt` - 文件下载器
  - `UpdateInstaller.kt` - APK 安装器
  - 更新 `SettingsScreen.kt` - 集成更新功能

## 许可证

MIT License
