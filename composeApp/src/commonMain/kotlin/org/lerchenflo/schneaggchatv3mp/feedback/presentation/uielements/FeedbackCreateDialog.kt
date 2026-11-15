package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryChanges
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_bug_device_info_hint
import schneaggchatv3mp.composeapp.generated.resources.feedback_create_title_bug
import schneaggchatv3mp.composeapp.generated.resources.feedback_create_title_feature
import schneaggchatv3mp.composeapp.generated.resources.feedback_duplicate_warning
import schneaggchatv3mp.composeapp.generated.resources.feedback_premium
import schneaggchatv3mp.composeapp.generated.resources.feedback_submit

/**
 * Form for a new feature request or bug report. Always opens with a reminder to look for
 * duplicates first. Only here can a feature be marked as premium.
 */
@Composable
fun FeedbackCreateDialog(
    initialType: FeedbackType,
    isSubmitting: Boolean,
    onSubmit: (NewFeedbackEntry) -> Unit,
    onDismiss: () -> Unit,
    initialTags: List<FeedbackTag> = emptyList(),
) {
    var isPremium by rememberSaveable { mutableStateOf(false) }

    FeedbackEntryFormDialog(
        heading = { type ->
            stringResource(
                if (type == FeedbackType.FEATURE) Res.string.feedback_create_title_feature
                else Res.string.feedback_create_title_bug
            )
        },
        initial = FeedbackEntryChanges(
            type = initialType,
            title = "",
            description = "",
            tags = initialTags,
            location = null,
        ),
        isSubmitting = isSubmitting,
        submitLabel = stringResource(Res.string.feedback_submit),
        onSubmit = { form ->
            onSubmit(
                NewFeedbackEntry(
                    type = form.type,
                    title = form.title,
                    description = form.description,
                    tags = form.tags,
                    location = form.location,
                    isPremium = isPremium && form.type == FeedbackType.FEATURE,
                )
            )
        },
        onDismiss = onDismiss,
        topHint = {
            FeedbackHintBox(
                text = stringResource(Res.string.feedback_duplicate_warning),
                isWarning = true,
            )
        },
        leadingChips = { type ->
            // Features only, and not one of the 1-3 tags (a separate flag on the server)
            if (type == FeedbackType.FEATURE) {
                FilterChip(
                    selected = isPremium,
                    onClick = { isPremium = !isPremium },
                    label = { Text(stringResource(Res.string.feedback_premium)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
            }
        },
        bottomHint = { type ->
            if (type == FeedbackType.BUG) {
                FeedbackHintBox(
                    text = stringResource(Res.string.feedback_bug_device_info_hint),
                    isWarning = false,
                )
            }
        },
    )
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
