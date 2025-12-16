package com.xmvisio.app.update

import android.util.Log
import com.xmvisio.app.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases API 更新检查器
 * 不需要 Token 也可以使用（60次/小时限制）
 * 如果提供 Token 可以提升到 5000次/小时
 */
class UpdateChecker(
    private val repositoryOwner: String = Constants.GITHUB_OWNER,
    private val repositoryName: String = Constants.GITHUB_REPO,
    private val githubToken: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    /**
     * 检查是否有新版本
     */
    suspend fun checkLatestVersion(currentVersion: String): NewVersion? = withContext(Dispatchers.IO) {
        try {
            val url = "${Constants.GITHUB_API_BASE_URL}/repos/$repositoryOwner/$repositoryName/releases/latest"
            Log.d("UpdateChecker", "请求URL: $url")
            Log.d("UpdateChecker", "当前版本: $currentVersion")
            
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
            
            // 如果提供了Token，添加Authorization header
            if (!githubToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $githubToken")
                Log.d("UpdateChecker", "使用GitHub Token进行认证请求")
            } else {
                Log.d("UpdateChecker", "使用未认证请求（限制60次/小时）")
            }
            
            val request = requestBuilder.get().build()
            val response = client.newCall(request).execute()
            
            // 检查 rate limit
            val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull() ?: -1
            Log.d("UpdateChecker", "HTTP响应码: ${response.code}, RateLimit剩余: $remaining")
            
            if (!response.isSuccessful) {
                if (response.code == 403 && remaining == 0) {
                    throw IOException("GitHub API 请求次数已达上限，请稍后重试")
                }
                Log.e("UpdateChecker", "HTTP请求失败: ${response.code}")
                return@withContext null
            }
            
            val body = response.body?.string() ?: run {
                Log.e("UpdateChecker", "响应体为空")
                return@withContext null
            }
            
            val release = json.decodeFromString<GitHubRelease>(body)
            Log.d("UpdateChecker", "最新Release: tagName=${release.tagName}, assets数量=${release.assets.size}")
            
            val latestVersion = release.tagName.removePrefix("v")
            val compareResult = compareVersions(latestVersion, currentVersion)
            Log.d("UpdateChecker", "版本比较: $latestVersion vs $currentVersion = $compareResult")
            
            // 比较版本号
            if (compareResult > 0) {
                // 查找 APK 下载链接
                val apkAsset = release.assets.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true)
                }
                
                if (apkAsset != null) {
                    val githubUrl = apkAsset.browserDownloadUrl
                    val jsDelivrUrl = convertToJsDelivrUrl(
                        githubUrl = githubUrl,
                        tagName = release.tagName,
                        fileName = apkAsset.name
                    )
                    
                    val newVersion = NewVersion(
                        version = latestVersion,
                        name = release.name.ifEmpty { release.tagName },
                        changelog = release.body,
                        downloadUrl = jsDelivrUrl,  // 优先使用jsDelivr
                        publishedAt = release.publishedAt,
                        fallbackUrl = githubUrl  // 保存原始URL用于回退
                    )
                    Log.d("UpdateChecker", "返回NewVersion: version=${newVersion.version}")
                    return@withContext newVersion
                } else {
                    Log.w("UpdateChecker", "未找到APK资源文件")
                }
            }
            
            null
        } catch (e: IOException) {
            Log.e("UpdateChecker", "IOException: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Exception: ${e.message}", e)
            throw IOException("检查更新失败: ${e.message}", e)
        }
    }
    
    /**
     * 将GitHub Release下载URL转换为jsDelivr CDN URL
     */
    private fun convertToJsDelivrUrl(githubUrl: String, tagName: String, fileName: String): String {
        return try {
            val githubPattern = Regex("https://github.com/([^/]+)/([^/]+)/releases/download/(.+)")
            val matchResult = githubPattern.find(githubUrl)
            
            if (matchResult != null) {
                val (owner, repo, _) = matchResult.destructured
                val jsDelivrUrl = "${Constants.CDN_BASE_URL}/gh/$owner/$repo@$tagName/$fileName"
                Log.d("UpdateChecker", "URL转换: $githubUrl -> $jsDelivrUrl")
                jsDelivrUrl
            } else {
                val jsDelivrUrl = "${Constants.CDN_BASE_URL}/gh/$repositoryOwner/$repositoryName@$tagName/$fileName"
                Log.d("UpdateChecker", "使用默认格式构建URL: $jsDelivrUrl")
                jsDelivrUrl
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "URL转换失败: ${e.message}", e)
            githubUrl
        }
    }
    
    /**
     * 比较版本号
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }
            val compare = v1Part.compareTo(v2Part)
            if (compare != 0) {
                return compare
            }
        }
        return 0
    }
}

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val body: String,
    @SerialName("published_at")
    val publishedAt: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Long
)

data class NewVersion(
    val version: String,
    val name: String,
    val changelog: String,
    val downloadUrl: String,  // 优先使用的URL（jsDelivr CDN）
    val publishedAt: String,
    val fallbackUrl: String? = null  // 回退URL（GitHub原始URL）
)
