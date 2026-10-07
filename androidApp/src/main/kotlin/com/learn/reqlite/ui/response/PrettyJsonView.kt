package com.learn.reqlite.ui.response

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.ui.response.json.JsonTreeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PrettyJsonParsedData(
    val formattedJson: String,
    val lines: List<String>
)

private const val INITIAL_CHUNK_LIMIT = 2000

@Composable
fun PrettyJsonView(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var wrapLines by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val parsedData by produceState<PrettyJsonParsedData?>(initialValue = null, rawJson) {
        value = null
        value = withContext(Dispatchers.IO) {
            val formatted = try {
                JsonTreeParser.formatPrettyJson(rawJson)
            } catch (_: Throwable) {
                rawJson
            }
            val lines = formatted.lines()
            PrettyJsonParsedData(formattedJson = formatted, lines = lines)
        }
    }

    val formattedJson = parsedData?.formattedJson ?: ""
    val lines = parsedData?.lines ?: emptyList()
    var displayAll by remember(rawJson) { mutableStateOf(false) }

    val displayCount = if (displayAll || lines.size <= INITIAL_CHUNK_LIMIT) {
        lines.size
    } else {
        INITIAL_CHUNK_LIMIT
    }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Actions bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Formatted JSON (${lines.size} lines)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = wrapLines,
                    onClick = { wrapLines = !wrapLines },
                    label = { Text("Wrap", fontSize = 11.sp) }
                )

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(formattedJson))
                        copiedNotice = "Copied JSON to clipboard"
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Copy JSON", fontSize = 12.sp)
                }
            }
        }

        if (copiedNotice != null) {
            Text(
                text = copiedNotice ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (parsedData == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    val contentModifier = if (wrapLines) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    }

                    LazyColumn(
                        modifier = contentModifier
                    ) {
                        items(displayCount) { index ->
                            val line = lines[index]
                            val annotated = remember(line, isDark) {
                                JsonTreeParser.highlightJsonSyntax(line, isDark = isDark)
                            }
                            SelectionContainer {
                                Text(
                                    text = annotated,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    softWrap = wrapLines
                                )
                            }
                        }

                        if (!displayAll && lines.size > INITIAL_CHUNK_LIMIT) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Showing first $INITIAL_CHUNK_LIMIT of ${lines.size} lines for performance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FilledTonalButton(
                                        onClick = { displayAll = true },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text("Load remaining ${lines.size - INITIAL_CHUNK_LIMIT} lines", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
