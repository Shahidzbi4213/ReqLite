package com.learn.reqlite.ui.workspace
import com.learn.reqlite.ui.response.ResponseCache

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.ui.environment.EnvironmentSelector
import com.learn.reqlite.ui.environment.ProtectedEnvironmentWarningDialog
import com.learn.reqlite.ui.environment.WorkspaceTopBar
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.automirrored.filled.Send
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import com.learn.reqlite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    initialUrl: String? = null,
    initialMethod: String = "GET",
    initialDraftId: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToResponse: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = koinViewModel()
) {
    val url by viewModel.url.collectAsState()
    val method by viewModel.method.collectAsState()
    val queryParams by viewModel.queryParams.collectAsState()
    val headers by viewModel.headers.collectAsState()
    val body by viewModel.body.collectAsState()
    val authConfiguration by viewModel.authConfiguration.collectAsState()

    val executionState by viewModel.executionState.collectAsState()
    val environments by viewModel.environments.collectAsState()
    val activeEnvironment by viewModel.activeEnvironment.collectAsState()
    val isProtectedWarningVisible by viewModel.isProtectedWarningVisible.collectAsState()
    val pendingExecution by viewModel.pendingExecution.collectAsState()

    val openDrafts by viewModel.openDrafts.collectAsState()
    val activeDraftId by viewModel.activeDraftId.collectAsState()
    val collections by viewModel.collections.collectAsState()
    var showTabsSheet by remember { mutableStateOf(false) }
    var showCurlDialog by remember { mutableStateOf(false) }
    var showSaveCollectionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val handled = viewModel.applySmartPayload(result.contents)
            if (!handled) {
                viewModel.setUrl(result.contents)
                viewModel.setMethod("GET")
            }
            Toast.makeText(context, "Scanned & applied successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(initialDraftId) {
        if (!initialDraftId.isNullOrBlank()) {
            viewModel.loadDraft(initialDraftId)
        }
    }

    LaunchedEffect(initialUrl, initialMethod) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.setUrl(initialUrl)
            viewModel.setMethod(initialMethod)
        }
    }

    LaunchedEffect(executionState) {
        if (executionState is ExecutionUiState.Success) {
            val successState = executionState as ExecutionUiState.Success
            ResponseCache.currentResponse = successState.response
            onNavigateToResponse(successState.historyEntry.id)
            viewModel.resetExecutionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    EnvironmentSelector(
                        activeEnvironment = activeEnvironment,
                        environments = environments,
                        onSelectEnvironment = { viewModel.selectEnvironment(it) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveDraft(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTabsSheet = true }) {
                         Icon(androidx.compose.material.icons.Icons.Default.Menu, contentDescription = "Open Tabs")
                    }
                    IconButton(onClick = { showSaveCollectionDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_collection),
                            contentDescription = "Save to Collection",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showCurlDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_terminal),
                            contentDescription = "Paste cURL",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val options = ScanOptions()
                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        options.setPrompt("Scan a QR Code")
                        options.setCameraId(0) // Use a specific camera of the device
                        options.setBeepEnabled(false)
                        options.setBarcodeImageEnabled(true)
                        qrScannerLauncher.launch(options)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_qr_code_scanner),
                            contentDescription = "Scan QR Code",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (maxWidth > 600.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f)) {
                        WorkspaceHeader(
                            url = url,
                            onUrlChange = { viewModel.setUrl(it) },
                            method = method,
                            onMethodChange = { viewModel.setMethod(it) }
                        )
                        RequestEditor(
                            queryParams = queryParams,
                            headers = headers,
                            body = body,
                            authConfiguration = authConfiguration,
                            onQueryParamsChange = { viewModel.setQueryParams(it) },
                            onHeadersChange = { viewModel.setHeaders(it) },
                            onBodyChange = { viewModel.setBody(it) },
                            onAuthChange = { viewModel.setAuthConfiguration(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ExecutionStatePanel(
                            state = executionState,
                            onSendClicked = { viewModel.onSendClicked() },
                            onCancelClicked = { viewModel.cancelExecution() }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f)) {
                        WorkspaceHeader(
                            url = url,
                            onUrlChange = { viewModel.setUrl(it) },
                            method = method,
                            onMethodChange = { viewModel.setMethod(it) }
                        )
                        RequestEditor(
                            queryParams = queryParams,
                            headers = headers,
                            body = body,
                            authConfiguration = authConfiguration,
                            onQueryParamsChange = { viewModel.setQueryParams(it) },
                            onHeadersChange = { viewModel.setHeaders(it) },
                            onBodyChange = { viewModel.setBody(it) },
                            onAuthChange = { viewModel.setAuthConfiguration(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(16.dp)
                    ) {
                        ExecutionStatePanel(
                            state = executionState,
                            onSendClicked = { viewModel.onSendClicked() },
                            onCancelClicked = { viewModel.cancelExecution() }
                        )
                    }
                }
            }
        }
    }

    // Protected Environment Warning Dialog
    if (isProtectedWarningVisible && pendingExecution != null) {
        val pending = pendingExecution!!
        ProtectedEnvironmentWarningDialog(
            environment = pending.environment,
            method = pending.method,
            url = pending.url,
            onConfirm = { viewModel.confirmProtectedExecution() },
            onDismiss = { viewModel.dismissProtectedWarning() }
        )
    }
    
    // Tabs Bottom Sheet
    if (showTabsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTabsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Open Requests (${openDrafts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text("Create New Request", color = MaterialTheme.colorScheme.primary) },
                            leadingContent = {
                                Icon(Icons.Default.Add, contentDescription = "New", tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                viewModel.createNewTab()
                                showTabsSheet = false
                            }
                        )
                        HorizontalDivider()
                    }
                    items(openDrafts) { draft ->
                        val isActive = draft.id == activeDraftId
                        ListItem(
                            headlineContent = { Text(draft.url.ifBlank { "Untitled Request" }) },
                            supportingContent = { Text(draft.method.name) },
                            leadingContent = {
                                if (isActive) {
                                    Icon(Icons.Default.Check, contentDescription = "Active")
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.switchTab(draft.id)
                                showTabsSheet = false
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
    }

    if (showCurlDialog) {
        var curlInput by remember { mutableStateOf("") }
        val clipboardManager = LocalClipboardManager.current
        
        AlertDialog(
            onDismissRequest = { showCurlDialog = false },
            title = { Text("Import cURL Command") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a cURL command to import URL, method, headers, auth token, and request body.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = curlInput,
                        onValueChange = { curlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("curl -X POST https://api.example.com ...") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                curlInput = clipText
                            }
                        }) {
                            Text("Paste Clipboard")
                        }
                        if (curlInput.isNotBlank()) {
                            TextButton(onClick = { curlInput = "" }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.applyCurl(curlInput)
                        if (success) {
                            Toast.makeText(context, "cURL imported successfully", Toast.LENGTH_SHORT).show()
                            showCurlDialog = false
                        } else {
                            Toast.makeText(context, "Failed to parse cURL command", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = curlInput.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveCollectionDialog) {
        SaveToCollectionDialog(
            collections = collections,
            initialUrl = url,
            onDismiss = { showSaveCollectionDialog = false },
            onSave = { colId, newColName, reqName ->
                viewModel.saveToCollection(
                    collectionId = colId,
                    newCollectionName = newColName,
                    requestName = reqName
                ) {
                    Toast.makeText(context, "Saved to collection", Toast.LENGTH_SHORT).show()
                    showSaveCollectionDialog = false
                }
            }
        )
    }
}

@Composable
fun SaveToCollectionDialog(
    collections: List<com.learn.reqlite.domain.model.Collection>,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (collectionId: String, newCollectionName: String?, requestName: String) -> Unit
) {
    var requestName by remember { mutableStateOf(initialUrl.ifBlank { "New Request" }) }
    var isCreatingNew by remember { mutableStateOf(collections.isEmpty()) }
    var newCollectionName by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf(collections.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Collection", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = requestName,
                    onValueChange = { requestName = it },
                    label = { Text("Request Name") },
                    singleLine = true,
                    shape = com.learn.reqlite.ui.theme.PillShape,
                    modifier = Modifier.fillMaxWidth()
                )

                if (collections.isEmpty() || isCreatingNew) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("New Collection Name") },
                        placeholder = { Text("e.g. My API") },
                        singleLine = true,
                        shape = com.learn.reqlite.ui.theme.PillShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (collections.isNotEmpty()) {
                        TextButton(
                            onClick = { isCreatingNew = false },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Choose existing collection", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text("Select Collection:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        collections.forEach { col ->
                            val isSelected = selectedCollectionId == col.id
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCollectionId = col.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { isCreatingNew = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+ Create New Collection", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReqName = requestName.trim().ifBlank { initialUrl.ifBlank { "Untitled Request" } }
                    if (isCreatingNew || collections.isEmpty()) {
                        onSave("", newCollectionName.trim(), finalReqName)
                    } else {
                        onSave(selectedCollectionId, null, finalReqName)
                    }
                },
                enabled = if (isCreatingNew || collections.isEmpty()) newCollectionName.isNotBlank() else selectedCollectionId.isNotBlank(),
                shape = com.learn.reqlite.ui.theme.PillShape
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
