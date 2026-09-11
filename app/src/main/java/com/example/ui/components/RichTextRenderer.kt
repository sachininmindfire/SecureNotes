package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RichTextRenderer(
    content: String,
    onToggleChecklist: ((Int) -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val lines = content.lines()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var inCodeBlock = false
        val codeBlockBuffer = StringBuilder()

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()

            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    val code = codeBlockBuffer.toString().trimEnd()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(end = 36.dp)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    codeBlockBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeBlockBuffer.clear()
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(rawLine).append("\n")
                return@forEachIndexed
            }

            // Horizontal Divider
            if (line.trim() == "---" || line.trim() == "***") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                return@forEachIndexed
            }

            // Headings
            if (line.startsWith("# ")) {
                Text(
                    text = parseInlineMarkdown(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            } else if (line.startsWith("## ")) {
                Text(
                    text = parseInlineMarkdown(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            } else if (line.startsWith("### ")) {
                Text(
                    text = parseInlineMarkdown(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            // Checklist item: - [ ] or - [x]
            else if (line.trimStart().startsWith("- [ ] ") || line.trimStart().startsWith("- [x] ") || line.trimStart().startsWith("- [X] ")) {
                val isChecked = line.trimStart().startsWith("- [x] ") || line.trimStart().startsWith("- [X] ")
                val itemText = line.trimStart().removePrefix("- [ ] ").removePrefix("- [x] ").removePrefix("- [X] ")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            onToggleChecklist?.invoke(index)
                        },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = parseInlineMarkdown(itemText),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isChecked) textColor.copy(alpha = 0.55f) else textColor
                    )
                }
            }
            // Blockquote: >
            else if (line.startsWith("> ")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("> ")),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            }
            // Bullet list: - or *
            else if (line.startsWith("- ") || line.startsWith("* ")) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 7.dp, end = 8.dp)
                            .size(7.dp)
                    )
                    Text(
                        text = parseInlineMarkdown(line.substring(2)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
            // Numbered list
            else if (line.matches(Regex("^\\d+\\.\\s+.*"))) {
                val number = line.substringBefore(".")
                val rest = line.substringAfter(". ")
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "$number.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = parseInlineMarkdown(rest),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
            // Regular text / paragraph
            else if (line.isNotBlank()) {
                Text(
                    text = parseInlineMarkdown(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 22.sp
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Parses bold (**text**), italic (*text*), and inline code (`text`) into an AnnotatedString.
 */
fun parseInlineMarkdown(raw: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = raw.length

        while (i < len) {
            // Bold: **text**
            if (raw.startsWith("**", i)) {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    val content = raw.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(content)
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Inline Code: `text`
            if (raw[i] == '`') {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    val content = raw.substring(i + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22888888)
                        )
                    )
                    append(" $content ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Italic: *text*
            if (raw[i] == '*' && (i + 1 < len && raw[i + 1] != '*')) {
                val end = raw.indexOf('*', i + 1)
                if (end != -1) {
                    val content = raw.substring(i + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                    i = end + 1
                    continue
                }
            }

            append(raw[i])
            i++
        }
    }
}
