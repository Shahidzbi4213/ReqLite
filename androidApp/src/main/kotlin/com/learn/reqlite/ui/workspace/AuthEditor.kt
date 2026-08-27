package com.learn.reqlite.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField

sealed interface AuthConfiguration {
    data object None : AuthConfiguration
    data class Bearer(val token: String) : AuthConfiguration
    data class Basic(val username: String, val password: String) : AuthConfiguration
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthEditor(
    authConfiguration: AuthConfiguration,
    onAuthChange: (AuthConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    val authTypes = listOf("None", "Bearer Token", "Basic Auth")
    val selectedText = when (authConfiguration) {
        is AuthConfiguration.None -> "None"
        is AuthConfiguration.Bearer -> "Bearer Token"
        is AuthConfiguration.Basic -> "Basic Auth"
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
                label = "Auth Type",
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                authTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            expanded = false
                            val newAuth = when (type) {
                                "None" -> AuthConfiguration.None
                                "Bearer Token" -> AuthConfiguration.Bearer("")
                                "Basic Auth" -> AuthConfiguration.Basic("", "")
                                else -> AuthConfiguration.None
                            }
                            onAuthChange(newAuth)
                        }
                    )
                }
            }
        }

        when (authConfiguration) {
            is AuthConfiguration.None -> {
                Text(
                    text = "This request does not use any authorization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is AuthConfiguration.Bearer -> {
                ReqLiteTextField(
                    value = authConfiguration.token,
                    onValueChange = { onAuthChange(AuthConfiguration.Bearer(it)) },
                    label = "Token",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is AuthConfiguration.Basic -> {
                ReqLiteTextField(
                    value = authConfiguration.username,
                    onValueChange = { onAuthChange(authConfiguration.copy(username = it)) },
                    label = "Username",
                    modifier = Modifier.fillMaxWidth()
                )
                ReqLiteTextField(
                    value = authConfiguration.password,
                    onValueChange = { onAuthChange(authConfiguration.copy(password = it)) },
                    label = "Password",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
