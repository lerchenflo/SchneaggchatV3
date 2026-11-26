package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.runtime.Composable
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
import schneaggchatv3mp.composeapp.generated.resources.feedback_submit

/**
 * Form for a new feature request or bug report. Always opens with a reminder to look for
 * duplicates first.
 */
@Composable
fun FeedbackCreateDialog(
    initialType: FeedbackType,
    isSubmitting: Boolean,
    onSubmit: (NewFeedbackEntry) -> Unit,
    onDismiss: () -> Unit,
    initialTags: List<FeedbackTag> = emptyList(),
) {
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
            isPremium = false,
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
                    isPremium = form.storedPremium,
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
