package com.learn.reqlite.ui.response

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.ui.response.json.JsonTreeParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PrettyJsonView(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    val isDark = isSystemInDarkTheme()

    val parseResult by produceState<Pair<String, List<AnnotatedString>>?>(initialValue = null, rawJson, isDark) {
        value = null
        value = withContext(Dispatchers.Default) {
            val formatted = JsonTreeParser.formatPrettyJson(rawJson)
            val highlightedLines = JsonTreeParser.highlightJsonSyntaxLines(formatted, isDark = isDark)
            formatted to highlightedLines
        }
    }

    val formattedJson = parseResult?.first ?: ""
    val highlightedLines = parseResult?.second

    val verticalScrollState = rememberScrollState()
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
                text = "Formatted JSON (${formattedJson.lines().size} lines)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (highlightedLines == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    SelectionContainer {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            items(highlightedLines.size) { index ->
                                Text(
                                    text = highlightedLines[index],
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
