package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType

@Composable
fun LogsDialog(
    logs: List<LogEntry>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<LogType?>(null) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) {
            logs
        } else {
            logs.filter { it.type == selectedFilter }
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Application Logs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = onClearLogs,
                            enabled = logs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear logs"
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${logs.size})") }
                    )

                    LogType.entries.forEach { type ->
                        val count = logs.count { it.type == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = type },
                            label = { Text("${type.name} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getLogColor(type).copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                HorizontalDivider()

                // Log list
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No logs available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogEntryItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: LogEntry) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Log type badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = getLogColor(log.type),
                modifier = Modifier.align(Alignment.Top)
            ) {
                Text(
                    text = log.type.name.first().toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Log message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = log.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = getLogColor(log.type),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            // Copy button
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(log.message))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy, // or use a custom icon
                    contentDescription = "Copy log message",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun getLogColor(type: LogType): Color {
    return when (type) {
        LogType.INFO -> Color(0xFF2196F3)
        LogType.WARNING -> Color(0xFFFFA726)
        LogType.ERROR -> Color(0xFFEF5350)
        LogType.DEBUG -> Color(0xFF66BB6A)
    }
}