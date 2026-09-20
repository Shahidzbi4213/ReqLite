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

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.model.Request
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRequest: (url: String?, method: String?, draftId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val recentRequests by viewModel.recentRequests.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val savedRequests by viewModel.savedRequests.collectAsState()
    val context = LocalContext.current

    HomeScreenContent(
        recentRequests = recentRequests,
        drafts = drafts,
        collections = collections,
        savedRequests = savedRequests,
        onNavigateToRequest = onNavigateToRequest,
        onImportCurl = { cmd ->
            viewModel.importCurl(cmd) { draftId ->
                onNavigateToRequest(null, null, draftId)
            }
        },
        onCreateCollection = { name ->
            viewModel.createCollection(name)
        },
        onDeleteCollection = { col ->
            viewModel.deleteCollection(col)
        },
        onDeleteRequest = { id ->
            viewModel.deleteRequest(id)
        },
        onOpenSavedRequest = { req ->
            viewModel.openSavedRequest(req) { draftId ->
                onNavigateToRequest(null, null, draftId)
            }
        },
        onImportPostmanCollection = { json ->
            viewModel.importPostmanCollection(json) { result ->
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    Toast.makeText(context, "Imported collection with $count requests!", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Import failed"
                    Toast.makeText(context, "Import failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    recentRequests: List<HistoryEntry>,
    drafts: List<Draft>,
    collections: List<Collection> = emptyList(),
    savedRequests: List<Request> = emptyList(),
    onNavigateToRequest: (url: String?, method: String?, draftId: String?) -> Unit,
    onImportCurl: (String) -> Unit = {},
    onCreateCollection: (String) -> Unit = {},
    onDeleteCollection: (Collection) -> Unit = {},
    onDeleteRequest: (String) -> Unit = {},
    onOpenSavedRequest: (Request) -> Unit = {},
    onImportPostmanCollection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var quickUrl by remember { mutableStateOf("") }
    var curlCommand by remember { mutableStateOf("") }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showImportPostmanDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    val expandedCollectionIds = remember { mutableStateListOf<String>() }

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
                onClick = { onNavigateToRequest(null, "GET", null) },
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
                    onSubmit = { onNavigateToRequest(quickUrl, "GET", null) }
                )
            }

            item {
                CurlEntrySection(
                    curl = curlCommand,
                    onCurlChange = { curlCommand = it },
                    onSubmit = {
                        onImportCurl(curlCommand)
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Collections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showImportPostmanDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Import", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { showCreateCollectionDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (collections.isEmpty()) {
                item {
                    EmptyStateCard(message = "No collections yet. Organize your requests into collections.")
                }
            } else {
                items(collections, key = { it.id }) { collection ->
                    val isExpanded = expandedCollectionIds.contains(collection.id)
                    val colRequests = savedRequests.filter { it.collectionId == collection.id }

                    CollectionCard(
                        collection = collection,
                        requests = colRequests,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            if (isExpanded) {
                                expandedCollectionIds.remove(collection.id)
                            } else {
                                expandedCollectionIds.add(collection.id)
                            }
                        },
                        onOpenRequest = onOpenSavedRequest,
                        onDeleteRequest = onDeleteRequest,
                        onDeleteCollection = { onDeleteCollection(collection) }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
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
                        onClick = { onNavigateToRequest(recent.requestUrl, recent.requestMethod.name, null) }
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
                        onClick = { onNavigateToRequest(draft.url, draft.method.name, draft.id) }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }
        }

        if (showCreateCollectionDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateCollectionDialog = false
                    newCollectionName = ""
                },
                title = { Text("New Collection", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a name for your API collection:")
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            placeholder = { Text("e.g. Users API") },
                            singleLine = true,
                            shape = PillShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newCollectionName.trim()
                            if (trimmed.isNotBlank()) {
                                onCreateCollection(trimmed)
                                showCreateCollectionDialog = false
                                newCollectionName = ""
                            }
                        },
                        enabled = newCollectionName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateCollectionDialog = false
                        newCollectionName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showImportPostmanDialog) {
            ImportPostmanDialog(
                onDismiss = { showImportPostmanDialog = false },
                onImport = { json ->
                    showImportPostmanDialog = false
                    onImportPostmanCollection(json)
                }
            )
        }
    }
}

@Composable
fun ImportPostmanDialog(
    onDismiss: () -> Unit,
    onImport: (jsonContent: String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var previewInfo by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Postman Collection", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a Postman v2.0 or v2.1 collection JSON below to import all endpoints, folders, and headers into ReqLite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        if (it.isNotBlank()) {
                            val parser = com.learn.reqlite.domain.parser.PostmanCollectionParserImpl()
                            when (val res = parser.parse(it)) {
                                is com.learn.reqlite.domain.parser.PostmanParseResult.Success -> {
                                    previewInfo = "${res.collection.name} • ${res.requests.size} requests • ${res.folders.size} folders"
                                    isError = false
                                }
                                is com.learn.reqlite.domain.parser.PostmanParseResult.Error -> {
                                    previewInfo = res.message
                                    isError = true
                                }
                            }
                        } else {
                            previewInfo = null
                            isError = false
                        }
                    },
                    label = { Text("Collection JSON") },
                    placeholder = { Text("{\"info\": {\"name\": ...}, \"item\": [...]}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp),
                    maxLines = 8,
                    isError = isError
                )

                if (previewInfo != null) {
                    Text(
                        text = previewInfo ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.isNotBlank() && !isError) {
                        onImport(jsonText.trim())
                    }
                },
                enabled = jsonText.isNotBlank() && !isError
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
fun CollectionCard(
    collection: Collection,
    requests: List<Request>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenRequest: (Request) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onDeleteCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val desc = collection.description
                    if (!desc.isNullOrBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${requests.size} requests",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteCollection,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Collection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                if (requests.isEmpty()) {
                    Text(
                        text = "No saved requests in this collection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        requests.forEach { req ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenRequest(req) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HttpMethodBadge(method = req.method.name)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = req.name.ifBlank { req.url },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = req.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteRequest(req.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
