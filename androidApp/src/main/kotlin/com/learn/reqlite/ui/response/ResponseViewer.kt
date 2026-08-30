package com.learn.reqlite.ui.response

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ResponseBodyMode {
    TREE,
    PRETTY,
    RAW
}

@Composable
fun ResponseViewer(
    response: HttpResponseUiModel,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    initialBodyMode: ResponseBodyMode = ResponseBodyMode.TREE
) {
    var selectedTabIndex by remember { mutableStateOf(initialTab) }
    var selectedBodyMode by remember(response) {
        mutableStateOf(
            if (response.isJsonContentType) initialBodyMode else ResponseBodyMode.RAW
        )
    }

    val tabs = listOf(
        "Body",
        if (response.headers.isNotEmpty()) "Headers (${response.headers.size})" else "Headers",
        "Summary"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Status & Metadata Bar
        ResponseStatusSummary(
            response = response,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // Main Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Body View with mode selector
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Body Mode Selector Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedBodyMode == ResponseBodyMode.TREE,
                                onClick = { selectedBodyMode = ResponseBodyMode.TREE },
                                label = { Text("JSON Tree", fontSize = 12.sp) }
                            )

                            FilterChip(
                                selected = selectedBodyMode == ResponseBodyMode.PRETTY,
                                onClick = { selectedBodyMode = ResponseBodyMode.PRETTY },
                                label = { Text("Pretty JSON", fontSize = 12.sp) }
                            )

                            FilterChip(
                                selected = selectedBodyMode == ResponseBodyMode.RAW,
                                onClick = { selectedBodyMode = ResponseBodyMode.RAW },
                                label = { Text("Raw Text", fontSize = 12.sp) }
                            )
                        }

                        // Selected Body Mode Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedBodyMode) {
                                ResponseBodyMode.TREE -> JsonTreeView(rawJson = response.body)
                                ResponseBodyMode.PRETTY -> PrettyJsonView(rawJson = response.body)
                                ResponseBodyMode.RAW -> RawTextView(rawText = response.body)
                            }
                        }
                    }
                }
                1 -> {
                    ResponseHeadersView(headers = response.headers)
                }
                2 -> {
                    ResponseSummaryView(response = response)
                }
            }
        }
    }
}
