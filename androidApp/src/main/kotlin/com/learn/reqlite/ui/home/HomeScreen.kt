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
import com.learn.reqlite.R
import com.learn.reqlite.ui.designsystem.components.ReqLiteButton
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRequest: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickUrl by remember { mutableStateOf("") }
    var curlCommand by remember { mutableStateOf("") }

    val recents = listOf("GET https://api.example.com/users", "POST https://api.example.com/login")
    val drafts = listOf("PUT https://api.example.com/profile")

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
            FloatingActionButton(onClick = { onNavigateToRequest(null) }) {
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
                    onSubmit = { onNavigateToRequest(quickUrl) }
                )
            }

            item {
                CurlEntrySection(
                    curl = curlCommand,
                    onCurlChange = { curlCommand = it },
                    onSubmit = { /* Handle cURL parsing later */ }
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

            items(recents) { recent ->
                RequestCard(title = recent, onClick = { onNavigateToRequest(recent) })
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }

            item {
                Text(
                    text = stringResource(R.string.drafts),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.small)
                )
            }

            items(drafts) { draft ->
                RequestCard(title = draft, onClick = { onNavigateToRequest(draft) })
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
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
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
