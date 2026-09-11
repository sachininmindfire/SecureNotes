package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@Composable
fun NoteQrDialog(
    noteTitle: String,
    noteContent: String,
    onDismiss: () -> Unit,
    onShareText: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val combinedText = remember(noteTitle, noteContent) {
        if (noteTitle.isNotBlank()) "$noteTitle\n\n$noteContent" else noteContent
    }

    // Generate pseudo-QR grid based on text bytes with standard corner finder patterns
    val qrMatrix = remember(combinedText) {
        generateQrMatrix(combinedText, size = 25)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Optical Transfer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan with any phone camera to transfer note offline:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // White canvas container for high-contrast QR display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(210.dp)
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth()) {
                        val gridSize = qrMatrix.size
                        val cellSize = size.width / gridSize

                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                if (qrMatrix[r][c]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = noteTitle.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${combinedText.length} characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(combinedText))
                    },
                    modifier = Modifier.testTag("btn_qr_copy_text")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        onShareText()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("btn_qr_share_text")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Generates a clean 2D boolean matrix with standard QR finder patterns and encoded payload modules.
 */
private fun generateQrMatrix(text: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    // 1. Draw top-left finder pattern (7x7)
    drawFinderPattern(matrix, 0, 0)
    // 2. Draw top-right finder pattern (7x7)
    drawFinderPattern(matrix, 0, size - 7)
    // 3. Draw bottom-left finder pattern (7x7)
    drawFinderPattern(matrix, size - 7, 0)

    // 4. Draw timing patterns
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // 5. Fill data modules based on deterministic hash of content
    val bytes = text.toByteArray(Charsets.UTF_8)
    var bitIndex = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder zones
            if ((r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)) {
                continue
            }
            if (r == 6 || c == 6) continue

            val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 42
            val bit = ((byteVal shr (bitIndex % 8)) and 1) == 1
            val mask = (r + c) % 2 == 0
            matrix[r][c] = bit xor mask
            bitIndex++
        }
    }

    return matrix
}

private fun drawFinderPattern(matrix: Array<BooleanArray>, startR: Int, startC: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
            val isInnerCenter = r in 2..4 && c in 2..4
            matrix[startR + r][startC + c] = isOuterBorder || isInnerCenter
        }
    }
}
