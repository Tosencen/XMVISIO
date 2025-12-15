# GitHub Token 配置指南（可选）

## 📌 重要说明

**GitHub Token 是可选的！** 默认情况下应用可以正常工作。

- **无 Token**：60 次/小时/IP（足够个人使用）
- **有 Token**：5000 次/小时（适合高频使用）

## 🔧 配置步骤

### 1. 创建 GitHub Personal Access Token

1. 访问：https://github.com/settings/tokens
2. 点击 **"Generate new token"** → **"Generate new token (classic)"**
3. 配置 Token：
   - **Note**: `XMVISIO Update Checker`
   - **Expiration**: 选择 `No expiration` 或自定义时间
   - **Scopes**: **只勾选 `public_repo`**（只读公开仓库权限）
4. 点击 **"Generate token"**
5. **立即复制 Token**（格式：`ghp_xxxxxxxxxxxx`）
   - ⚠️ Token 只显示一次，请妥善保存！

### 2. 配置到项目

#### 方法 A：使用 local.properties（推荐）

在项目根目录的 `local.properties` 文件中添加：

```properties
# GitHub Token for update checker (optional)
github.token=ghp_your_token_here
```

**优点**：
- ✅ 不会提交到 Git（`local.properties` 已在 `.gitignore` 中）
- ✅ 安全，不会泄露到公开仓库
- ✅ 每个开发者可以使用自己的 Token

#### 方法 B：环境变量（CI/CD 使用）

如果需要在 CI/CD 中使用，可以设置环境变量：

```bash
export ORG_GRADLE_PROJECT_github_token=ghp_your_token_here
```

### 3. 验证配置

重新构建项目：

```bash
./gradlew clean
./gradlew :app:android:assembleDebug
```

查看日志，应该看到：
```
UpdateViewModel: 使用 GitHub Token 进行 API 请求
```

## 🔒 安全建议

1. **永远不要**将 Token 提交到 Git 仓库
2. **永远不要**在代码中硬编码 Token
3. **定期轮换** Token（建议每 6 个月）
4. **最小权限**：只授予 `public_repo` 权限
5. 如果 Token 泄露，立即在 GitHub 上撤销

## 📊 Token 使用情况

### 无 Token
- 限制：60 次/小时/IP
- 适用场景：
  - 个人使用
  - 用户手动检查更新
  - 低频率更新检查

### 有 Token
- 限制：5000 次/小时
- 适用场景：
  - 应用用户量大
  - 需要频繁检查更新
  - 自动后台更新检查

## 🧪 测试 Token

可以使用以下命令测试 Token 是否有效：

```bash
curl -H "Authorization: token ghp_your_token_here" \
  https://api.github.com/repos/Tosencen/XMVISIO/releases/latest
```

成功响应示例：
```json
{
  "tag_name": "v1.0.0",
  "name": "XMVISIO v1.0.0",
  ...
}
```

## ❓ 常见问题

### Q: 不配置 Token 会怎样？
A: 完全正常工作！只是 API 请求限制为 60 次/小时。

### Q: Token 过期了怎么办？
A: 重新生成一个新的 Token，更新 `local.properties` 即可。

### Q: 多个开发者如何配置？
A: 每个开发者在自己的 `local.properties` 中配置自己的 Token。

### Q: 发布应用时需要 Token 吗？
A: 不需要！用户使用应用时不需要 Token，60 次/小时足够。

## 📝 相关文件

- `app/android/build.gradle.kts` - BuildConfig 配置
- `app/android/src/main/kotlin/com/xmvisio/app/update/UpdateViewModel.kt` - Token 使用
- `app/android/src/main/kotlin/com/xmvisio/app/update/UpdateChecker.kt` - API 请求
- `local.properties` - Token 存储位置（不提交到 Git）

## 🎯 总结

**推荐做法**：
1. 开发阶段：不配置 Token（60 次/小时够用）
2. 测试阶段：如果频繁测试，配置 Token
3. 发布阶段：不需要 Token（用户端使用）

**只有在以下情况才需要配置 Token**：
- 开发时频繁测试更新功能
- 应用有大量用户同时检查更新
- 需要自动化 CI/CD 测试

对于大多数情况，**不需要配置 Token**！
