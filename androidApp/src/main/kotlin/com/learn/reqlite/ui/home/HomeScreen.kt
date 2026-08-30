package com.learn.reqlite.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.learn.reqlite.R
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.parser.CurlParseResult
import com.learn.reqlite.domain.parser.CurlParserImpl
import com.learn.reqlite.ui.designsystem.components.ReqLiteButton
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
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
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToRequest(null, "GET") }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_request))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.small)
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
    val (containerColor, contentColor) = when (method.uppercase()) {
        "GET" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "POST" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "PUT" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "DELETE" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "PATCH" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = method.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QuickRequestSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.quick_request),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                ReqLiteTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = stringResource(R.string.quick_request_hint),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                FilledIconButton(onClick = onSubmit) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send_request))
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.import_from_curl),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            ReqLiteTextField(
                value = curl,
                onValueChange = onCurlChange,
                placeholder = stringResource(R.string.curl_hint),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            
            ReqLiteButton(
                onClick = onSubmit,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.import_action))
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
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (durationMs != null || statusCode != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (statusCode != null) {
                            val statusColor = if (statusCode in 200..299) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = "$statusCode",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(MaterialTheme.spacing.medium)
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
