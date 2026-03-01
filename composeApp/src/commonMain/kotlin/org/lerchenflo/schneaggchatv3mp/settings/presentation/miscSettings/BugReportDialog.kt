package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_actual
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_description
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_expected
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_steps
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_steps_placeholder
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_field_title
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_label_priority
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_label_severity
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_priority_high
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_priority_low
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_priority_medium
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_severity_critical
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_severity_high
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_severity_low
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_severity_medium
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_title_bug
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_title_feature
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_type_bug
import schneaggchatv3mp.composeapp.generated.resources.bugreportdialog_type_feature
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok

enum class ReportType { BUG, FEATURE }
enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
enum class Priority { LOW, MEDIUM, HIGH }

@Composable
fun BugReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reportType by remember { mutableStateOf(ReportType.BUG) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stepsToReproduce by remember { mutableStateOf("") }
    var expectedBehavior by remember { mutableStateOf("") }
    var actualBehavior by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(Severity.MEDIUM) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    val isValid = description.isNotBlank() && (reportType == ReportType.BUG || title.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (reportType == ReportType.BUG) Res.string.bugreportdialog_title_bug
                    else Res.string.bugreportdialog_title_feature
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- Type Toggle ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReportType.entries.forEach { type ->
                        FilterChip(
                            selected = reportType == type,
                            onClick = { reportType = type },
                            label = {
                                Text(
                                    stringResource(
                                        if (type == ReportType.BUG) Res.string.bugreportdialog_type_bug
                                        else Res.string.bugreportdialog_type_feature
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (type == ReportType.BUG) Icons.Default.BugReport else Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                // --- Shared description ---
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.bugreportdialog_field_description)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Bug-specific fields ---
                AnimatedVisibility(visible = reportType == ReportType.BUG) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        OutlinedTextField(
                            value = stepsToReproduce,
                            onValueChange = { stepsToReproduce = it },
                            label = { Text(stringResource(Res.string.bugreportdialog_field_steps)) },
                            placeholder = { Text(stringResource(Res.string.bugreportdialog_field_steps_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = expectedBehavior,
                            onValueChange = { expectedBehavior = it },
                            label = { Text(stringResource(Res.string.bugreportdialog_field_expected)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = actualBehavior,
                            onValueChange = { actualBehavior = it },
                            label = { Text(stringResource(Res.string.bugreportdialog_field_actual)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(Res.string.bugreportdialog_label_severity),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Severity.entries.forEach { s ->
                                FilterChip(
                                    selected = severity == s,
                                    onClick = { severity = s },
                                    label = {
                                        Text(
                                            text = stringResource(
                                                when (s) {
                                                    Severity.LOW -> Res.string.bugreportdialog_severity_low
                                                    Severity.MEDIUM -> Res.string.bugreportdialog_severity_medium
                                                    Severity.HIGH -> Res.string.bugreportdialog_severity_high
                                                    Severity.CRITICAL -> Res.string.bugreportdialog_severity_critical
                                                }
                                            ),
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Feature-specific fields ---
                AnimatedVisibility(visible = reportType == ReportType.FEATURE) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(Res.string.bugreportdialog_field_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(Res.string.bugreportdialog_label_priority),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Priority.entries.forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = {
                                        Text(
                                            text = stringResource(
                                                when (p) {
                                                    Priority.LOW -> Res.string.bugreportdialog_priority_low
                                                    Priority.MEDIUM -> Res.string.bugreportdialog_priority_medium
                                                    Priority.HIGH -> Res.string.bugreportdialog_priority_high
                                                }
                                            ),
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        formatReport(
                            reportType = reportType,
                            title = title,
                            description = description,
                            stepsToReproduce = stepsToReproduce,
                            expectedBehavior = expectedBehavior,
                            actualBehavior = actualBehavior,
                            severity = severity,
                            priority = priority
                        )
                    )
                },
                enabled = isValid
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

private fun formatReport(
    reportType: ReportType,
    title: String,
    description: String,
    stepsToReproduce: String,
    expectedBehavior: String,
    actualBehavior: String,
    severity: Severity,
    priority: Priority
): String = buildString {
    if (reportType == ReportType.BUG) {
        appendLine("🐛 BUG REPORT")
        appendLine("=============")
        appendLine("Severity: ${severity.name}")
        appendLine()
        appendLine("Description:")
        appendLine(description)
        if (stepsToReproduce.isNotBlank()) {
            appendLine()
            appendLine("Steps to Reproduce:")
            appendLine(stepsToReproduce)
        }
        if (expectedBehavior.isNotBlank()) {
            appendLine()
            appendLine("Expected Behavior:")
            appendLine(expectedBehavior)
        }
        if (actualBehavior.isNotBlank()) {
            appendLine()
            appendLine("Actual Behavior:")
            appendLine(actualBehavior)
        }
    } else {
        appendLine("💡 FEATURE REQUEST")
        appendLine("==================")
        appendLine("Title: $title")
        appendLine("Priority: ${priority.name}")
        appendLine()
        appendLine("Description:")
        appendLine(description)
    }
}