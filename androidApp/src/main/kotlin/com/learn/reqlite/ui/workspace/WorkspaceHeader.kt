package com.learn.reqlite.ui.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.R
import com.learn.reqlite.ui.theme.MethodColors
import com.learn.reqlite.ui.theme.PillShape
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
    val isDark = isSystemInDarkTheme()
    val currentMethodColors = MethodColors.forMethod(method, isDark)

    val animatedContainerColor by animateColorAsState(
        targetValue = currentMethodColors.container,
        label = "method_container_anim"
    )
    val animatedOnContainerColor by animateColorAsState(
        targetValue = currentMethodColors.onContainer,
        label = "method_text_anim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Method Badge Pill
            Box {
                Surface(
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { expanded = true },
                    shape = PillShape,
                    color = animatedContainerColor,
                    border = BorderStroke(1.dp, currentMethodColors.border.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = method,
                            color = animatedOnContainerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select HTTP Method",
                            tint = animatedOnContainerColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(160.dp)
                ) {
                    methods.forEach { itemMethod ->
                        val itemColors = MethodColors.forMethod(itemMethod, isDark)
                        val isSelected = itemMethod.equals(method, ignoreCase = true)

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(10.dp),
                                        shape = PillShape,
                                        color = itemColors.border
                                    ) {}
                                    Text(
                                        text = itemMethod,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            onClick = {
                                onMethodChange(itemMethod)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Material 3 Expressive URL Field
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        text = "https://api.example.com/v1/resource",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = PillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
            )
        }
    }
}
