package com.learn.reqlite.ui.workspace
import com.learn.reqlite.ui.response.ResponseCache

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.widget.Toast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    initialUrl: String? = null,
    initialMethod: String = "GET",
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

    val context = LocalContext.current
    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.setUrl(result.contents)
            viewModel.setMethod("GET")
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
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
            WorkspaceTopBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val options = ScanOptions()
                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        options.setPrompt("Scan a QR Code")
                        options.setCameraId(0) // Use a specific camera of the device
                        options.setBeepEnabled(false)
                        options.setBarcodeImageEnabled(true)
                        qrScannerLauncher.launch(options)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Scan QR Code")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = { },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (executionState is ExecutionUiState.Loading) {
                                viewModel.cancelExecution()
                            } else {
                                viewModel.onSendClicked()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        if (executionState is ExecutionUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
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
                            .padding(8.dp)
                    ) {
                        ExecutionStatePanel(state = executionState)
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
                            .weight(1.2f)
                            .padding(8.dp)
                    ) {
                        ExecutionStatePanel(state = executionState)
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
}
