package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun RichTextEditorToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onAddImageClick: () -> Unit,
    onColorPickerClick: () -> Unit,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bold
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                testTag = "toolbar_bold"
            ) {
                onValueChange(wrapSelection(textFieldValue, "**", "**"))
            }

            // Italic
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                testTag = "toolbar_italic"
            ) {
                onValueChange(wrapSelection(textFieldValue, "*", "*"))
            }

            // Heading 1
            ToolbarButton(
                icon = Icons.Default.Title,
                description = "Heading",
                testTag = "toolbar_heading"
            ) {
                onValueChange(insertAtLineStart(textFieldValue, "## "))
            }

            // Checklist task
            ToolbarButton(
                icon = Icons.Default.CheckBox,
                description = "Checklist Task",
                testTag = "toolbar_checklist"
            ) {
                onValueChange(insertAtLineStart(textFieldValue, "- [ ] "))
            }

            // Bullet list
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                description = "Bullet List",
                testTag = "toolbar_bullet"
            ) {
                onValueChange(insertAtLineStart(textFieldValue, "- "))
            }

            // Numbered list
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                description = "Numbered List",
                testTag = "toolbar_numbered"
            ) {
                onValueChange(insertAtLineStart(textFieldValue, "1. "))
            }

            // Quote
            ToolbarButton(
                icon = Icons.Default.FormatQuote,
                description = "Quote",
                testTag = "toolbar_quote"
            ) {
                onValueChange(insertAtLineStart(textFieldValue, "> "))
            }

            // Code
            ToolbarButton(
                icon = Icons.Default.Code,
                description = "Code",
                testTag = "toolbar_code"
            ) {
                if (textFieldValue.selection.collapsed) {
                    onValueChange(insertText(textFieldValue, "\n```\n// Code here\n```\n"))
                } else {
                    onValueChange(wrapSelection(textFieldValue, "`", "`"))
                }
            }

            // Horizontal Divider
            ToolbarButton(
                icon = Icons.Default.HorizontalRule,
                description = "Divider",
                testTag = "toolbar_divider"
            ) {
                onValueChange(insertText(textFieldValue, "\n---\n"))
            }

            // Image Attachment
            ToolbarButton(
                icon = Icons.Default.AddPhotoAlternate,
                description = "Add Image",
                testTag = "toolbar_add_image",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                onAddImageClick()
            }

            // Note Color Theme
            ToolbarButton(
                icon = Icons.Default.Palette,
                description = "Note Color",
                testTag = "toolbar_palette"
            ) {
                onColorPickerClick()
            }

            // Security Padlock Lock
            ToolbarButton(
                icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                description = if (isLocked) "Locked Note" else "Unlocked Note",
                testTag = "toolbar_toggle_lock",
                containerColor = if (isLocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (isLocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            ) {
                onToggleLock()
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    testTag: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .testTag(testTag),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun wrapSelection(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    return if (selection.collapsed) {
        val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
        TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length)
        )
    } else {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
        TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        )
    }
}

private fun insertAtLineStart(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val lineStart = text.lastIndexOf('\n', (selection.start - 1).coerceAtLeast(0)).let {
        if (it == -1) 0 else it + 1
    }
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(
        text = newText,
        selection = TextRange(selection.start + prefix.length)
    )
}

private fun insertText(value: TextFieldValue, insert: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val newText = text.substring(0, selection.start) + insert + text.substring(selection.end)
    return TextFieldValue(
        text = newText,
        selection = TextRange(selection.start + insert.length)
    )
}
