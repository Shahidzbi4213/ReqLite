package com.learn.reqlite.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learn.reqlite.domain.graphql.model.GraphQLSchema
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.workspace.graphql.GraphQLQueryEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBodyEditor(
    body: RequestBody,
    onBodyChange: (RequestBody) -> Unit,
    schema: GraphQLSchema? = null,
    onFetchSchema: (() -> Unit)? = null,
    isLoadingSchema: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bodyTypes = listOf("None", "Text", "Form Data", "URL Encoded", "GraphQL")
    val selectedText = when (body) {
        is RequestBody.NoBody -> "None"
        is RequestBody.TextBody -> "Text"
        is RequestBody.FormDataBody -> "Form Data"
        is RequestBody.UrlEncodedBody -> "URL Encoded"
        is RequestBody.GraphQLBody -> "GraphQL"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            ReqLiteTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                label = "Body Type",
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                bodyTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            expanded = false
                            val newBody = when (type) {
                                "None" -> RequestBody.NoBody
                                "Text" -> RequestBody.TextBody("", "application/json")
                                "Form Data" -> RequestBody.FormDataBody(emptyList())
                                "URL Encoded" -> RequestBody.UrlEncodedBody(emptyList())
                                "GraphQL" -> RequestBody.GraphQLBody(query = "", variables = "{}")
                                else -> RequestBody.NoBody
                            }
                            onBodyChange(newBody)
                        }
                    )
                }
            }
        }

        when (body) {
            is RequestBody.NoBody -> {
                Text(
                    text = "This request does not have a body.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is RequestBody.TextBody -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReqLiteTextField(
                        value = body.contentType,
                        onValueChange = { onBodyChange(body.copy(contentType = it)) },
                        label = "Content Type",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ReqLiteTextField(
                    value = body.content,
                    onValueChange = { onBodyChange(body.copy(content = it)) },
                    label = "Content",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    singleLine = false
                )
            }
            is RequestBody.FormDataBody -> {
                RequestFieldEditor(
                    fields = body.parts,
                    onFieldsChange = { onBodyChange(body.copy(parts = it)) },
                    addButtonText = "Add Form Field"
                )
            }
            is RequestBody.UrlEncodedBody -> {
                RequestFieldEditor(
                    fields = body.fields,
                    onFieldsChange = { onBodyChange(body.copy(fields = it)) },
                    addButtonText = "Add URL Encoded Field"
                )
            }
            is RequestBody.GraphQLBody -> {
                GraphQLQueryEditor(
                    body = body,
                    onBodyChange = onBodyChange,
                    schema = schema,
                    onFetchSchema = onFetchSchema,
                    isLoadingSchema = isLoadingSchema
                )
            }
        }
    }
}
