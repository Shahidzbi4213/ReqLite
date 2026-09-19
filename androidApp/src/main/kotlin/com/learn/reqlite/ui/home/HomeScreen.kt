package com.learn.reqlite.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.R
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.parser.CurlParseResult
import com.learn.reqlite.domain.parser.CurlParserImpl
import com.learn.reqlite.ui.designsystem.components.ReqLiteButton
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.theme.MethodColors
import com.learn.reqlite.ui.theme.PillShape
import com.learn.reqlite.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRequest: (url: String?, method: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val recentRequests by viewModel.recentRequests.collectAsState()
    val drafts by viewModel.drafts.collectAsState()

    HomeScreenContent(
        recentRequests = recentRequests,
        drafts = drafts,
        onNavigateToRequest = onNavigateToRequest,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    recentRequests: List<HistoryEntry>,
    drafts: List<Draft>,
    onNavigateToRequest: (url: String?, method: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickUrl by remember { mutableStateOf("") }
    var curlCommand by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToRequest(null, "GET") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_request), fontWeight = FontWeight.SemiBold) },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(top = MaterialTheme.spacing.small, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            item {
                QuickRequestSection(
                    url = quickUrl,
                    onUrlChange = { quickUrl = it },
                    onSubmit = { onNavigateToRequest(quickUrl, "GET") }
                )
            }

            item {
                CurlEntrySection(
                    curl = curlCommand,
                    onCurlChange = { curlCommand = it },
                    onSubmit = {
                        val result = CurlParserImpl().parse(curlCommand)
                        if (result is CurlParseResult.Success) {
                            onNavigateToRequest(result.draft.url, result.draft.method.name)
                        } else {
                            onNavigateToRequest(curlCommand.trim(), "GET")
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.recent_requests),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.extraSmall)
                )
            }

            if (recentRequests.isEmpty()) {
                item {
                    EmptyStateCard(message = "No recent requests yet. Execute an API call to see history here.")
                }
            } else {
                items(recentRequests, key = { it.id }) { recent ->
                    RequestCard(
                        method = recent.requestMethod.name,
                        url = recent.requestUrl,
                        statusCode = recent.statusCode,
                        durationMs = recent.durationMs,
                        onClick = { onNavigateToRequest(recent.requestUrl, recent.requestMethod.name) }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.drafts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.extraSmall)
                )
            }

            if (drafts.isEmpty()) {
                item {
                    EmptyStateCard(message = "No drafts saved.")
                }
            } else {
                items(drafts, key = { it.id }) { draft ->
                    RequestCard(
                        method = draft.method.name,
                        url = draft.url,
                        onClick = { onNavigateToRequest(draft.url, draft.method.name) }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }
        }
    }
}

@Composable
fun HttpMethodBadge(
    method: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val methodColors = MethodColors.forMethod(method, isDark)

    Surface(
        color = methodColors.container,
        shape = PillShape,
        border = BorderStroke(1.dp, methodColors.border.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = method.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = methodColors.onContainer,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun QuickRequestSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(R.string.quick_request),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.quick_request_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = PillShape,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                FilledIconButton(
                    onClick = onSubmit,
                    shape = PillShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_request),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CurlEntrySection(
    curl: String,
    onCurlChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(R.string.import_from_curl),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = curl,
                onValueChange = onCurlChange,
                placeholder = {
                    Text(
                        stringResource(R.string.curl_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = onSubmit,
                shape = PillShape,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.import_action), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCard(
    method: String,
    url: String,
    statusCode: Int? = null,
    durationMs: Long? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            HttpMethodBadge(method = method)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = url.ifBlank { "Untitled Request" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (durationMs != null || statusCode != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (statusCode != null) {
                            val isSuccess = statusCode in 200..299
                            val statusBg = if (isSuccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                            val statusText = if (isSuccess) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                            Surface(
                                shape = PillShape,
                                color = statusBg
                            ) {
                                Text(
                                    text = "$statusCode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (durationMs != null) {
                            Text(
                                text = "${durationMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(MaterialTheme.spacing.large)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
