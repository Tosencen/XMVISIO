package com.xmvisio.app.util

import java.util.regex.Pattern

/**
 * URL 工具类
 * 参考 Seal 项目实现
 */
object UrlUtil {
    
    private const val URL_REGEX_PATTERN =
        "(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?"
    
    /**
     * 从字符串中提取所有 URL
     * @param input 输入字符串
     * @param firstMatchOnly 是否只匹配第一个 URL
     * @return URL 列表
     */
    fun findURLsFromString(input: String, firstMatchOnly: Boolean = false): List<String> {
        val result = mutableListOf<String>()
        val pattern = Pattern.compile(URL_REGEX_PATTERN)
        
        with(pattern.matcher(input)) {
            if (!firstMatchOnly) {
                while (find()) {
                    result += group()
                }
            } else {
                if (find()) result += (group())
            }
        }
        return result
    }
    
    /**
     * 检查字符串是否包含 URL
     */
    fun containsUrl(input: String): Boolean {
        return findURLsFromString(input, firstMatchOnly = true).isNotEmpty()
    }
    
    /**
     * 提取第一个 URL
     */
    fun extractFirstUrl(input: String): String? {
        return findURLsFromString(input, firstMatchOnly = true).firstOrNull()
    }
}
