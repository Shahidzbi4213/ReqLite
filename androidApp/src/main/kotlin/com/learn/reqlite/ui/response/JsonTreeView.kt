package com.learn.reqlite.ui.response

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.reqlite.ui.designsystem.components.ReqLiteTextField
import com.learn.reqlite.ui.response.json.JsonTreeNode
import com.learn.reqlite.ui.response.json.JsonTreeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun JsonTreeView(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var rootNode by remember { mutableStateOf<JsonTreeNode?>(null) }
    var allExpandablePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var visibleItems by remember { mutableStateOf<List<FlattenedTreeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(rawJson) {
        isLoading = true
        withContext(Dispatchers.Default) {
            val node = JsonTreeParser.parseJsonToTree(rawJson)
            if (node != null) {
                val paths = mutableSetOf<String>()
                fun collectPaths(n: JsonTreeNode) {
                    when (n) {
                        is JsonTreeNode.ObjectNode -> {
                            paths.add(n.path)
                            n.children.forEach { collectPaths(it) }
                        }
                        is JsonTreeNode.ArrayNode -> {
                            paths.add(n.path)
                            n.children.forEach { collectPaths(it) }
                        }
                        is JsonTreeNode.PrimitiveNode -> {}
                    }
                }
                collectPaths(node)
                
                withContext(Dispatchers.Main) {
                    rootNode = node
                    allExpandablePaths = paths
                    expandedPaths = setOf(node.path)
                }
            } else {
                withContext(Dispatchers.Main) {
                    rootNode = null
                    allExpandablePaths = emptySet()
                    expandedPaths = emptySet()
                }
            }
        }
        isLoading = false
    }

    LaunchedEffect(rootNode, expandedPaths, searchQuery) {
        val root = rootNode
        if (root != null) {
            val items = withContext(Dispatchers.Default) {
                val list = mutableListOf<FlattenedTreeItem>()
                val pathsToKeep = mutableSetOf<String>()
                
                fun computeMatches(node: JsonTreeNode): Boolean {
                    val matchesSelf = if (searchQuery.isBlank()) true else {
                        (node.key?.contains(searchQuery, ignoreCase = true) == true) ||
                                (node is JsonTreeNode.PrimitiveNode && node.value.contains(searchQuery, ignoreCase = true))
                    }
                    var hasMatchingChild = false
                    when (node) {
                        is JsonTreeNode.ObjectNode -> node.children.forEach { if (computeMatches(it)) hasMatchingChild = true }
                        is JsonTreeNode.ArrayNode -> node.children.forEach { if (computeMatches(it)) hasMatchingChild = true }
                        is JsonTreeNode.PrimitiveNode -> {}
                    }
                    val matches = matchesSelf || hasMatchingChild
                    if (matches) {
                        pathsToKeep.add(node.path)
                    }
                    return matches
                }

                if (searchQuery.isNotBlank()) {
                    computeMatches(root)
                }

                fun flatten(node: JsonTreeNode, depth: Int) {
                    if (searchQuery.isNotBlank() && !pathsToKeep.contains(node.path)) return

                    val isExpanded = if (searchQuery.isNotBlank()) {
                        true // auto-expand when searching
                    } else {
                        expandedPaths.contains(node.path)
                    }
                    val isHighlighted = searchQuery.isNotBlank() && (
                            node.key?.contains(searchQuery, ignoreCase = true) == true ||
                                    (node is JsonTreeNode.PrimitiveNode && node.value.contains(searchQuery, ignoreCase = true))
                            )

                    val hasChildren = when (node) {
                        is JsonTreeNode.ObjectNode -> node.children.isNotEmpty()
                        is JsonTreeNode.ArrayNode -> node.children.isNotEmpty()
                        is JsonTreeNode.PrimitiveNode -> false
                    }

                    list.add(
                        FlattenedTreeItem(
                            node = node,
                            depth = depth,
                            isExpanded = isExpanded,
                            hasChildren = hasChildren,
                            isHighlighted = isHighlighted
                        )
                    )

                    if (isExpanded) {
                        when (node) {
                            is JsonTreeNode.ObjectNode -> node.children.forEach { flatten(it, depth + 1) }
                            is JsonTreeNode.ArrayNode -> node.children.forEach { flatten(it, depth + 1) }
                            is JsonTreeNode.PrimitiveNode -> {}
                        }
                    }
                }
                flatten(root, depth = 0)
                list
            }
            visibleItems = items
        } else {
            visibleItems = emptyList()
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (rootNode == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Cannot parse JSON tree",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The response body does not appear to be valid JSON. You can switch to Pretty or Raw text view.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Controls Row: Search & Expand/Collapse All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReqLiteTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search JSON tree...",
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search JSON"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                }
            )

            OutlinedButton(
                onClick = {
                    expandedPaths = if (expandedPaths.size >= allExpandablePaths.size) {
                        rootNode?.path?.let { setOf(it) } ?: emptySet() // collapse to root
                    } else {
                        allExpandablePaths.toSet() // expand all
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                val isAllExpanded = expandedPaths.size >= allExpandablePaths.size
                Text(if (isAllExpanded) "Collapse" else "Expand All", fontSize = 12.sp)
            }
        }

        if (copiedNotice != null) {
            Text(
                text = copiedNotice ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(visibleItems, key = { "${it.node.path}_${it.node.key}" }) { item ->
                JsonTreeNodeRow(
                    item = item,
                    onToggleExpand = {
                        expandedPaths = if (expandedPaths.contains(item.node.path)) {
                            expandedPaths - item.node.path
                        } else {
                            expandedPaths + item.node.path
                        }
                    },
                    onCopyPath = {
                        clipboardManager.setText(AnnotatedString(item.node.path))
                        copiedNotice = "Copied path: ${item.node.path}"
                    },
                    onCopyValue = {
                        val valueStr = when (val n = item.node) {
                            is JsonTreeNode.PrimitiveNode -> n.value
                            is JsonTreeNode.ObjectNode -> "{ ${n.size} properties }"
                            is JsonTreeNode.ArrayNode -> "[ ${n.size} items ]"
                        }
                        clipboardManager.setText(AnnotatedString(valueStr))
                        copiedNotice = "Copied value: $valueStr"
                    }
                )
            }
        }
    }
}

data class FlattenedTreeItem(
    val node: JsonTreeNode,
    val depth: Int,
    val isExpanded: Boolean,
    val hasChildren: Boolean,
    val isHighlighted: Boolean
)

@Composable
fun JsonTreeNodeRow(
    item: FlattenedTreeItem,
    onToggleExpand: () -> Unit,
    onCopyPath: () -> Unit,
    onCopyValue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indentPadding = (item.depth * 16).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (item.isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable {
                if (item.hasChildren) {
                    onToggleExpand()
                } else {
                    onCopyValue()
                }
            }
            .padding(start = indentPadding, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/Collapse Indicator
        if (item.hasChildren) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.isExpanded) "▼" else "▶",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        // Key Name
        if (!item.node.key.isNullOrBlank()) {
            Text(
                text = "${item.node.key}: ",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Value / Container Summary
        when (val node = item.node) {
            is JsonTreeNode.ObjectNode -> {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "{ ${node.size} keys }",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            is JsonTreeNode.ArrayNode -> {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "[ ${node.size} items ]",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            is JsonTreeNode.PrimitiveNode -> {
                val valueColor = when (node.type) {
                    JsonTreeNode.PrimitiveType.STRING -> Color(0xFF2E7D32)
                    JsonTreeNode.PrimitiveType.NUMBER -> Color(0xFF0277BD)
                    JsonTreeNode.PrimitiveType.BOOLEAN -> Color(0xFF8E24AA)
                    JsonTreeNode.PrimitiveType.NULL -> Color(0xFF757575)
                }
                val displayText = if (node.type == JsonTreeNode.PrimitiveType.STRING) "\"${node.value}\"" else node.value

                Text(
                    text = displayText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = valueColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Copy Path Button
        IconButton(
            onClick = onCopyPath,
            modifier = Modifier.size(24.dp)
        ) {
            Text("📍", fontSize = 11.sp)
        }
    }
}
