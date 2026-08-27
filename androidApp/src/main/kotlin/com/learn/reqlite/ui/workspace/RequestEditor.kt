package com.learn.reqlite.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.designsystem.components.ReqLiteButton
import java.util.UUID

@Composable
fun RequestEditor(
    queryParams: List<RequestField>,
    headers: List<RequestField>,
    body: RequestBody,
    authConfiguration: AuthConfiguration,
    onQueryParamsChange: (List<RequestField>) -> Unit,
    onHeadersChange: (List<RequestField>) -> Unit,
    onBodyChange: (RequestBody) -> Unit,
    onAuthChange: (AuthConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Params", "Headers", "Auth", "Body")

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> RequestFieldEditor(
                    fields = queryParams,
                    onFieldsChange = onQueryParamsChange,
                    addButtonText = "Add Param"
                )
                1 -> RequestFieldEditor(
                    fields = headers,
                    onFieldsChange = onHeadersChange,
                    addButtonText = "Add Header"
                )
                2 -> AuthEditor(
                    authConfiguration = authConfiguration,
                    onAuthChange = onAuthChange
                )
                3 -> RequestBodyEditor(
                    body = body,
                    onBodyChange = onBodyChange
                )
            }
        }
    }
}

@Composable
fun RequestFieldEditor(
    fields: List<RequestField>,
    onFieldsChange: (List<RequestField>) -> Unit,
    addButtonText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(fields) { index, field ->
                RequestFieldRow(
                    field = field,
                    onFieldChange = { updatedField ->
                        val newList = fields.toMutableList()
                        newList[index] = updatedField
                        onFieldsChange(newList)
                    },
                    onDelete = {
                        val newList = fields.toMutableList()
                        newList.removeAt(index)
                        onFieldsChange(newList)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ReqLiteButton(
            onClick = {
                val newList = fields.toMutableList()
                newList.add(
                    RequestField(
                        id = UUID.randomUUID().toString(),
                        key = "",
                        value = ""
                    )
                )
                onFieldsChange(newList)
            },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(addButtonText)
        }
    }
}

@Composable
fun RequestFieldRow(
    field: RequestField,
    onFieldChange: (RequestField) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = field.isEnabled,
            onCheckedChange = { isChecked ->
                onFieldChange(field.copy(isEnabled = isChecked))
            }
        )
        
        ReqLiteTextField(
            value = field.key,
            onValueChange = { onFieldChange(field.copy(key = it)) },
            label = "Key",
            modifier = Modifier.weight(1f)
        )
        
        ReqLiteTextField(
            value = field.value,
            onValueChange = { onFieldChange(field.copy(value = it)) },
            label = "Value",
            modifier = Modifier.weight(1f)
        )
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Field",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
