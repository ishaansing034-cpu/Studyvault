package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun RichTextToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconAction(
                icon = Icons.Filled.Title,
                description = "Heading",
                tag = "rich_heading",
                onClick = { onValueChange(prefixCurrentLine(value, "## ")) }
            )
            ToolbarIconAction(
                icon = Icons.Filled.FormatBold,
                description = "Bold",
                tag = "rich_bold",
                onClick = { onValueChange(wrapSelection(value, "**", "**", "bold text")) }
            )
            ToolbarIconAction(
                icon = Icons.Filled.FormatItalic,
                description = "Italic",
                tag = "rich_italic",
                onClick = { onValueChange(wrapSelection(value, "*", "*", "italic text")) }
            )
            ToolbarIconAction(
                icon = Icons.Filled.AutoFixHigh,
                description = "Highlight key definition",
                tag = "rich_highlight",
                onClick = { onValueChange(wrapSelection(value, "==", "==", "key definition")) }
            )
            ToolbarIconAction(
                icon = Icons.Filled.Functions,
                description = "Formula block",
                tag = "rich_formula",
                onClick = { onValueChange(wrapSelection(value, "`", "`", "V = I * R")) }
            )
            ToolbarIconAction(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                description = "Bullet point",
                tag = "rich_bullet",
                onClick = { onValueChange(prefixCurrentLine(value, "• ")) }
            )
            ToolbarIconAction(
                icon = Icons.Filled.FormatListNumbered,
                description = "Numbered item",
                tag = "rich_numbered",
                onClick = { onValueChange(prefixCurrentLine(value, "1. ")) }
            )
        }
    }
}

@Composable
private fun ToolbarIconAction(
    icon: ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun wrapSelection(
    tfv: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val text = tfv.text
    val start = tfv.selection.min.coerceIn(0, text.length)
    val end = tfv.selection.max.coerceIn(0, text.length)
    return if (start == end) {
        val inserted = "$prefix$placeholder$suffix"
        val newText = text.substring(0, start) + inserted + text.substring(end)
        val selStart = start + prefix.length
        val selEnd = selStart + placeholder.length
        TextFieldValue(newText, TextRange(selStart, selEnd))
    } else {
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
        val cursor = end + prefix.length + suffix.length
        TextFieldValue(newText, TextRange(cursor))
    }
}

private fun prefixCurrentLine(tfv: TextFieldValue, prefix: String): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        .let { if (it < 0 || cursor == 0) 0 else it + 1 }
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(newText, TextRange(cursor + prefix.length))
}

@Composable
fun RichTextViewer(
    rawText: String,
    modifier: Modifier = Modifier
) {
    val lines = rawText.split("\n")
    val highlightBg = MaterialTheme.colorScheme.tertiaryContainer
    val highlightFg = MaterialTheme.colorScheme.onTertiaryContainer
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val codeFg = MaterialTheme.colorScheme.secondary
    val headingColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trimStart()
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineRichText(
                            trimmed.removePrefix("# "),
                            highlightBg, highlightFg, codeBg, codeFg
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = headingColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                trimmed.startsWith("## ") || trimmed.startsWith("### ") -> {
                    val clean = trimmed.removePrefix("### ").removePrefix("## ")
                    Text(
                        text = parseInlineRichText(clean, highlightBg, highlightFg, codeBg, codeFg),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                trimmed.startsWith("```") -> Unit
                trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length > 2 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(codeBg)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = trimmed.removeSurrounding("`"),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 14.sp,
                            color = codeFg
                        )
                    }
                }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val body = trimmed.drop(2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineRichText(body, highlightBg, highlightFg, codeBg, codeFg),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Box(modifier = Modifier.padding(vertical = 2.dp))
                }
                else -> {
                    Text(
                        text = parseInlineRichText(rawLine, highlightBg, highlightFg, codeBg, codeFg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun parseInlineRichText(
    input: String,
    highlightBg: Color,
    highlightFg: Color,
    codeBg: Color,
    codeFg: Color
): AnnotatedString {
    val pattern = Regex("(\\*\\*.+?\\*\\*|==.+?==|`.+?`|\\*[^*]+?\\*)")
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in pattern.findAll(input)) {
            if (match.range.first > lastIndex) {
                append(input.substring(lastIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(token.removeSurrounding("**"))
                    }
                }
                token.startsWith("==") && token.endsWith("==") -> {
                    withStyle(
                        SpanStyle(
                            background = highlightBg,
                            color = highlightFg,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" ${token.removeSurrounding("==")} ")
                    }
                }
                token.startsWith("`") && token.endsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            background = codeBg,
                            color = codeFg,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" ${token.removeSurrounding("`")} ")
                    }
                }
                token.startsWith("*") && token.endsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(token.removeSurrounding("*"))
                    }
                }
                else -> append(token)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < input.length) {
            append(input.substring(lastIndex))
        }
    }
}
