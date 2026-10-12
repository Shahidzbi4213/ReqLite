package com.learn.reqlite.ui.workspace.graphql

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.domain.graphql.autocomplete.CompletionKind
import com.learn.reqlite.domain.graphql.autocomplete.GraphQLAutocompleteEngine
import com.learn.reqlite.domain.graphql.autocomplete.GraphQLCompletionItem
import com.learn.reqlite.domain.graphql.format.GraphQLFormatter
import com.learn.reqlite.domain.graphql.model.GraphQLSchema
import com.learn.reqlite.domain.model.RequestBody
import kotlinx.serialization.json.Json

@Composable
fun GraphQLQueryEditor(
    body: RequestBody.GraphQLBody,
    onBodyChange: (RequestBody.GraphQLBody) -> Unit,
    schema: GraphQLSchema? = null,
    onFetchSchema: (() -> Unit)? = null,
    isLoadingSchema: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSchemaExplorer by remember { mutableStateOf(false) }

    var queryTextFieldValue by remember(body.query) {
        mutableStateOf(TextFieldValue(body.query, TextRange(body.query.length)))
    }
    var variablesText by remember(body.variables) {
        mutableStateOf(body.variables)
    }

    val completions = remember(queryTextFieldValue.text, queryTextFieldValue.selection, schema) {
        GraphQLAutocompleteEngine.getCompletions(
            query = queryTextFieldValue.text,
            cursorOffset = queryTextFieldValue.selection.start,
            schema = schema
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toolbar with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.width(220.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Query", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Variables", fontSize = 13.sp) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedTab == 0) {
                    FilledTonalIconButton(
                        onClick = {
                            val formatted = GraphQLFormatter.format(queryTextFieldValue.text)
                            queryTextFieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                            onBodyChange(body.copy(query = formatted))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Prettify", modifier = Modifier.size(18.dp))
                    }
                }

                if (schema != null) {
                    Button(
                        onClick = { showSchemaExplorer = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Explorer", fontSize = 12.sp)
                    }
                } else if (onFetchSchema != null) {
                    OutlinedButton(
                        onClick = onFetchSchema,
                        enabled = !isLoadingSchema,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLoadingSchema) "Loading..." else "Introspect", fontSize = 12.sp)
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Autocomplete Suggestion Bar
            if (completions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    completions.take(15).forEach { item ->
                        SuggestionChip(
                            onClick = {
                                val currentText = queryTextFieldValue.text
                                val cursor = queryTextFieldValue.selection.start
                                val (startReplace, endReplace) = findWordBounds(currentText, cursor)

                                val newText = currentText.substring(0, startReplace) +
                                        item.insertText +
                                        currentText.substring(endReplace)
                                val newCursor = startReplace + item.insertText.length

                                queryTextFieldValue = TextFieldValue(newText, TextRange(newCursor))
                                onBodyChange(body.copy(query = newText))
                            },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.label,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    if (item.detail.isNotEmpty()) {
                                        Text(
                                            text = item.detail,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = when (item.kind) {
                                    CompletionKind.KEYWORD -> MaterialTheme.colorScheme.secondaryContainer
                                    CompletionKind.DIRECTIVE -> MaterialTheme.colorScheme.tertiaryContainer
                                    CompletionKind.ARGUMENT -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Query Text Area
            OutlinedTextField(
                value = queryTextFieldValue,
                onValueChange = { newValue ->
                    queryTextFieldValue = newValue
                    onBodyChange(body.copy(query = newValue.text))
                },
                placeholder = {
                    Text(
                        text = "query {\n  # Type query or choose from Schema Explorer\n}",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 350.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )
        } else {
            // Variables JSON Editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JSON Variables",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        val formatted = try {
                            val element = Json.parseToJsonElement(variablesText)
                            val prettyJson = Json { prettyPrint = true }
                            prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element)
                        } catch (_: Exception) {
                            variablesText
                        }
                        variablesText = formatted
                        onBodyChange(body.copy(variables = formatted))
                    }
                ) {
                    Text("Format JSON", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = variablesText,
                onValueChange = {
                    variablesText = it
                    onBodyChange(body.copy(variables = it))
                },
                placeholder = { Text("{\n  \"limit\": 10\n}", fontFamily = FontFamily.Monospace) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 250.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }

    if (showSchemaExplorer && schema != null) {
        GraphQLSchemaExplorerSheet(
            schema = schema,
            onDismissRequest = { showSchemaExplorer = false },
            onInsertSnippet = { snippet ->
                val currentText = queryTextFieldValue.text
                val newText = if (currentText.isBlank()) {
                    snippet
                } else {
                    currentText + "\n\n" + snippet
                }
                queryTextFieldValue = TextFieldValue(newText, TextRange(newText.length))
                onBodyChange(body.copy(query = newText))
            }
        )
    }
}

private fun findWordBounds(text: String, cursor: Int): Pair<Int, Int> {
    if (text.isEmpty() || cursor !in 0..text.length) return Pair(cursor, cursor)

    var start = cursor
    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_' || text[start - 1] == '@')) {
        start--
    }

    var end = cursor
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) {
        end++
    }

    return Pair(start, end)
}
