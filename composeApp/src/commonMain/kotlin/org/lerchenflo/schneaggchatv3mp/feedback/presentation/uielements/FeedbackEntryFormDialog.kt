package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.warningColors
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryChanges
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_description
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_description_bug_hint
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_location
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_tags
import schneaggchatv3mp.composeapp.generated.resources.feedback_field_title
import schneaggchatv3mp.composeapp.generated.resources.feedback_language_hint

// Same limits the server enforces
private const val TITLE_MIN_LENGTH = 3
private const val TITLE_MAX_LENGTH = 100
private const val DESCRIPTION_MAX_LENGTH = 2000
private const val LOCATION_MAX_LENGTH = 200
private const val MAX_TAGS = 3

/**
 * The entry form shared by [FeedbackCreateDialog] and [FeedbackEditDialog]: type, title,
 * description, location (features only) and 1-3 tags, starting from [initial].
 *
 * @param heading title of the dialog for the picked type
 * @param canSubmit extra condition on the filled form, on top of the field limits
 * @param topHint shown under the heading, for the picked type
 * @param leadingChips shown in front of the tag chips, for the picked type
 * @param bottomHint shown under the tags, for the picked type
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedbackEntryFormDialog(
    heading: @Composable (FeedbackType) -> String,
    initial: FeedbackEntryChanges,
    isSubmitting: Boolean,
    submitLabel: String,
    onSubmit: (FeedbackEntryChanges) -> Unit,
    onDismiss: () -> Unit,
    canSubmit: (FeedbackEntryChanges) -> Boolean = { true },
    topHint: @Composable (FeedbackType) -> Unit = {},
    leadingChips: @Composable FlowRowScope.(FeedbackType) -> Unit = {},
    bottomHint: @Composable (FeedbackType) -> Unit = {},
) {
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    var location by rememberSaveable { mutableStateOf(initial.location.orEmpty()) }
    var selectedTags by rememberSaveable { mutableStateOf(initial.tags) }

    val form = FeedbackEntryChanges(
        type = type,
        title = title,
        description = description,
        tags = selectedTags,
        location = location.takeIf { type == FeedbackType.FEATURE },
    )
    val isValid = title.trim().length >= TITLE_MIN_LENGTH &&
        description.isNotBlank() &&
        selectedTags.isNotEmpty() &&
        canSubmit(form)

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
                text = heading(type),
                style = MaterialTheme.typography.titleLarge,
            )

            topHint(type)

            CountSegmentedSwitch(
                options = FeedbackType.entries,
                selected = type,
                label = { stringResource(it.labelRes()) },
                count = { null },
                onSelect = { type = it },
            )

            // Entries in dialect or other languages are hard to read for everyone else
            Text(
                text = stringResource(Res.string.feedback_language_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                leadingChips(type)
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

            bottomHint(type)

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
                    onClick = { onSubmit(form) }
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(submitLabel)
                    }
                }
            }
        }
    }
}

/** Info or warning box of the entry form. */
@Composable
internal fun FeedbackHintBox(text: String, isWarning: Boolean) {
    val container = if (isWarning) MaterialTheme.warningColors.warningContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isWarning) MaterialTheme.warningColors.onWarningContainer else MaterialTheme.colorScheme.onSurfaceVariant

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
