package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryChanges
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTypeChangeEffect
import org.lerchenflo.schneaggchatv3mp.feedback.domain.typeChangeEffect
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_edit_entry
import schneaggchatv3mp.composeapp.generated.resources.feedback_save
import schneaggchatv3mp.composeapp.generated.resources.feedback_type_change_to_bug
import schneaggchatv3mp.composeapp.generated.resources.feedback_type_change_to_feature

/**
 * Edit form of an existing entry, filled with its current values. Picking another type shows what
 * the server resets; saving is possible once something changed.
 */
@Composable
fun FeedbackEditDialog(
    detail: FeedbackEntryDetail,
    isSubmitting: Boolean,
    onSubmit: (FeedbackEntryChanges) -> Unit,
    onDismiss: () -> Unit,
) {
    val entry = detail.entry
    // The values the form opened with; the stored entry may change behind the open form
    val initial = remember(entry.id) {
        FeedbackEntryChanges(
            type = entry.type,
            title = entry.title,
            description = detail.description,
            tags = entry.tags,
            location = detail.location,
            isPremium = entry.isPremium,
        )
    }

    FeedbackEntryFormDialog(
        heading = { stringResource(Res.string.feedback_edit_entry) },
        initial = initial,
        isSubmitting = isSubmitting,
        submitLabel = stringResource(Res.string.feedback_save),
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        canSubmit = { form -> form.differsFrom(initial) },
        topHint = { type ->
            entry.typeChangeEffect(type)?.let { effect ->
                FeedbackHintBox(text = stringResource(effect.warningRes()), isWarning = true)
            }
        },
    )
}

private fun FeedbackTypeChangeEffect.warningRes(): StringResource = when (this) {
    FeedbackTypeChangeEffect.TO_BUG -> Res.string.feedback_type_change_to_bug
    FeedbackTypeChangeEffect.TO_FEATURE -> Res.string.feedback_type_change_to_feature
}

@Preview
@Composable
private fun FeedbackEditDialogPreview() {
    SchneaggchatTheme {
        FeedbackEditDialog(
            detail = FeedbackEntryDetail(
                entry = previewFeedbackEntry,
                description = previewFeedbackEntry.description,
                location = "Map, top right",
                appVersion = null,
                platform = null,
                comments = emptyList(),
            ),
            isSubmitting = false,
            onSubmit = {},
            onDismiss = {},
        )
    }
}
