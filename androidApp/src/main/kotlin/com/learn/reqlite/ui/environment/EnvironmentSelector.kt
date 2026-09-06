package com.learn.reqlite.ui.environment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EnvironmentSelector(
    activeEnvironment: EnvironmentUiModel?,
    environments: List<EnvironmentUiModel>,
    onSelectEnvironment: (EnvironmentUiModel?) -> Unit,
    modifier: Modifier = Modifier,
    onManageEnvironments: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        EnvironmentBadge(
            activeEnvironment = activeEnvironment,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            // Header
            Text(
                text = "ENVIRONMENTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // No Environment option
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No Environment",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (activeEnvironment == null) FontWeight.Bold else FontWeight.Normal
                        )
                        if (activeEnvironment == null) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onClick = {
                    onSelectEnvironment(null)
                    expanded = false
                }
            )

            if (environments.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                environments.forEach { env ->
                    val isSelected = activeEnvironment?.id == env.id
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = env.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (env.isProtected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (env.isProtected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "🛡️ Protected",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else if (env.variableCount > 0) {
                                        Text(
                                            text = "${env.variableCount} vars",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectEnvironment(env)
                            expanded = false
                        }
                    )
                }
            }

            if (onManageEnvironments != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "⚙️ Manage Environments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        onManageEnvironments()
                    }
                )
            }
        }
    }
}
