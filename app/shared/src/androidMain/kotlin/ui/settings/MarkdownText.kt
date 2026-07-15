package com.xmvisio.app.ui.settings

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * 轻量 Markdown 渲染组件，用于更新弹窗的 changelog。
 * 支持：#/##/### 标题、**加粗**、*斜体* 或 _斜体_、`行内代码`、- 或 * 或 · 无序列表、1. 有序列表、[文本](链接)。
 * 不依赖任何外部库，避免对 KMP 依赖版本树造成冲击。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(markdown) {
        parseMarkdown(markdown, style, linkColor)
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.item
                ?.let { runCatching { uriHandler.openUri(it) } }
        }
    )
}

private fun parseMarkdown(
    markdown: String,
    base: TextStyle,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    val lines = markdown.replace("\r\n", "\n").lines()
    var orderedCounter = 0
    for (rawLine in lines) {
        val line = rawLine
        when {
            line.isBlank() -> {
                orderedCounter = 0
                append("\n")
            }
            line.startsWith("#") -> {
                orderedCounter = 0
                val (level, content) = parseHeading(line)
                val factor = when (level) {
                    1 -> 1.4f
                    2 -> 1.25f
                    else -> 1.12f
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = base.fontSize * factor)) {
                    appendInline(content, base, linkColor)
                }
                append("\n")
            }
            line.matches(Regex("^\\s*\\d+\\.\\s+.*")) -> {
                orderedCounter++
                val content = line.replace(Regex("^\\s*\\d+\\.\\s+"), "")
                append("  ${orderedCounter}. ")
                appendInline(content, base, linkColor)
                append("\n")
            }
            line.matches(Regex("^\\s*[-*·]\\s+.*")) -> {
                orderedCounter = 0
                val content = line.replace(Regex("^\\s*[-*·]\\s+"), "")
                append("  • ")
                appendInline(content, base, linkColor)
                append("\n")
            }
            else -> {
                orderedCounter = 0
                appendInline(line, base, linkColor)
                append("\n")
            }
        }
    }
}

private fun parseHeading(line: String): Pair<Int, String> {
    val hashes = line.takeWhile { it == '#' }.length
    val level = hashes.coerceIn(1, 3)
    val content = line.drop(hashes).trim()
    return level to content
}

private fun AnnotatedString.Builder.appendInline(text: String, base: TextStyle, linkColor: Color) {
    val regex = Regex("(\\*\\*(.+?)\\*\\*)|(\\*(.+?)\\*)|(_(.+?)_)|(`(.+?)`)|(\\[(.+?)\\]\\((.+?)\\))")
    var last = 0
    regex.findAll(text).forEach { m ->
        if (m.range.first > last) append(text.substring(last, m.range.first))
        when {
            m.groups[2] != null ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[2]) }
            m.groups[4] != null ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[4]) }
            m.groups[6] != null ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[6]) }
            m.groups[8] != null ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(m.groupValues[8]) }
            m.groups[9] != null -> {
                val linkText = m.groupValues[9]
                val url = m.groupValues[10]
                pushStringAnnotation("URL", url)
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(linkText)
                }
                pop()
            }
        }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}
