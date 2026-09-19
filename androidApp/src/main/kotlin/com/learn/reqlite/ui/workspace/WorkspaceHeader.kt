package com.learn.reqlite.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.learn.reqlite.R
import com.learn.reqlite.ui.designsystem.components.ReqLiteButton
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceHeader(
    url: String,
    onUrlChange: (String) -> Unit,
    method: String,
    onMethodChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // Method Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(0.35f)
            ) {
                ReqLiteTextField(
                    value = method,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.method),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    methods.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onMethodChange(selectionOption)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            // URL Input
            ReqLiteTextField(
                value = url,
                onValueChange = onUrlChange,
                label = stringResource(R.string.url),
                placeholder = "https://...",
                singleLine = true,
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}
