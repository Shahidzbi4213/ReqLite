package com.learn.reqlite.ui.response

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseScreen(
    historyId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResponseViewModel = koinViewModel()
) {
    val response by viewModel.response.collectAsState()

    LaunchedEffect(historyId) {
        viewModel.loadResponse(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Response") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (response != null) {
                ResponseViewer(
                    response = response!!,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
