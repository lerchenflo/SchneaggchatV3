package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.feedback_bug_device_info_hint
import schneaggchatv3mp.composeapp.generated.resources.feedback_create_title_bug
import schneaggchatv3mp.composeapp.generated.resources.feedback_create_title_feature
import schneaggchatv3mp.composeapp.generated.resources.feedback_duplicate_warning
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_description
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_description_bug_hint
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_location
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_tags
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_title
import schneaggchatv3mp.composeapp.generated.resources.feedback_submit

// Same limits the server enforces
private const val TITLE_MIN_LENGTH = 3
private const val TITLE_MAX_LENGTH = 100
private const val DESCRIPTION_MAX_LENGTH = 2000
private const val LOCATION_MAX_LENGTH = 200
private const val MAX_TAGS = 3

/**
 * Form for a new feature request or bug report. Always opens with a reminder to look for
 * duplicates first, since entries can't be edited or merged afterwards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackCreateDialog(
    initialType: FeedbackType,
    isSubmitting: Boolean,
    onSubmit: (NewFeedbackEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(initialType) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedTags by rememberSaveable { mutableStateOf(listOf<FeedbackTag>()) }

    val isValid = title.trim().length >= TITLE_MIN_LENGTH &&
        description.isNotBlank() &&
        selectedTags.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    if (type == FeedbackType.FEATURE) Res.string.feedback_create_title_feature
                    else Res.string.feedback_create_title_bug
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            HintBox(
                text = stringResource(Res.string.feedback_duplicate_warning),
                isWarning = true,
            )

            CountSegmentedSwitch(
                options = FeedbackType.entries,
                selected = type,
                label = { stringResource(it.labelRes()) },
                count = { null },
                onSelect = { type = it },
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= TITLE_MAX_LENGTH) title = it },
                label = { Text(stringResource(Res.string.feedback_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= DESCRIPTION_MAX_LENGTH) description = it },
                label = { Text(stringResource(Res.string.feedback_field_description)) },
                placeholder = {
                    if (type == FeedbackType.BUG) Text(stringResource(Res.string.feedback_field_description_bug_hint))
                },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == FeedbackType.FEATURE) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= LOCATION_MAX_LENGTH) location = it },
                    label = { Text(stringResource(Res.string.feedback_field_location)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = stringResource(Res.string.feedback_field_tags),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FeedbackTag.entries.forEach { tag ->
                    val selected = tag in selectedTags
                    FilterChip(
                        selected = selected,
                        enabled = selected || selectedTags.size < MAX_TAGS,
                        onClick = {
                            selectedTags = if (selected) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text(stringResource(tag.labelRes())) },
                    )
                }
            }

            if (type == FeedbackType.BUG) {
                HintBox(
                    text = stringResource(Res.string.feedback_bug_device_info_hint),
                    isWarning = false,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isValid && !isSubmitting,
                    onClick = {
                        onSubmit(
                            NewFeedbackEntry(
                                type = type,
                                title = title,
                                description = description,
                                tags = selectedTags,
                                location = location.takeIf { type == FeedbackType.FEATURE },
                            )
                        )
                    }
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.feedback_submit))
                    }
                }
            }
        }
    }
}

@Composable
private fun HintBox(text: String, isWarning: Boolean) {
    val container = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isWarning) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
            contentDescription = null,
            tint = content,
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}

@Preview
@Composable
private fun FeedbackCreateDialogPreview() {
    SchneaggchatTheme {
        FeedbackCreateDialog(
            initialType = FeedbackType.FEATURE,
            isSubmitting = false,
            onSubmit = {},
            onDismiss = {},
        )
    }
}
