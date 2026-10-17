package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.WheelRows
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementLimits
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.sharedUi.ChatSelectorDismissableInfo
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.announcement_delete_confirm
import schneaggchatv3mp.composeapp.generated.resources.announcement_edit_hint
import schneaggchatv3mp.composeapp.generated.resources.announcement_edit_title
import schneaggchatv3mp.composeapp.generated.resources.announcement_empty
import schneaggchatv3mp.composeapp.generated.resources.announcement_expired
import schneaggchatv3mp.composeapp.generated.resources.announcement_expires_at
import schneaggchatv3mp.composeapp.generated.resources.announcement_expiry
import schneaggchatv3mp.composeapp.generated.resources.announcement_expiry_custom
import schneaggchatv3mp.composeapp.generated.resources.announcement_expiry_days
import schneaggchatv3mp.composeapp.generated.resources.announcement_field_body
import schneaggchatv3mp.composeapp.generated.resources.announcement_field_title
import schneaggchatv3mp.composeapp.generated.resources.announcement_language_hint
import schneaggchatv3mp.composeapp.generated.resources.announcement_new
import schneaggchatv3mp.composeapp.generated.resources.announcement_preview
import schneaggchatv3mp.composeapp.generated.resources.announcement_required_language
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.dev_announcements
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val DATE_FORMAT = "dd.MM.yyyy HH:mm"

/**
 * Popup of the developer settings: the list of announcements, or the form to create / edit one.
 * Everyone with developer settings can open it; the server rejects non-admins.
 */
@Composable
fun AnnouncementAdminPopup(
    state: AnnouncementAdminState,
    onAction: (AnnouncementAdminAction) -> Unit,
) {
    Dialog(
        onDismissRequest = { onAction(AnnouncementAdminAction.Close) },
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
            val form = state.form
            if (form == null) {
                AnnouncementList(state = state, onAction = onAction)
            } else {
                AnnouncementFormContent(
                    form = form,
                    isSubmitting = state.isSubmitting,
                    error = state.error?.asString(),
                    onAction = onAction,
                )
            }
        }
    }

    state.deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { onAction(AnnouncementAdminAction.DeleteCancel) },
            text = { Text(stringResource(Res.string.announcement_delete_confirm, candidate.id.toString())) },
            confirmButton = {
                TextButton(onClick = { onAction(AnnouncementAdminAction.DeleteConfirm) }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AnnouncementAdminAction.DeleteCancel) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AnnouncementList(
    state: AnnouncementAdminState,
    onAction: (AnnouncementAdminAction) -> Unit,
) {
    Text(
        text = stringResource(Res.string.dev_announcements),
        style = MaterialTheme.typography.titleLarge,
    )

    state.error?.let { ErrorText(it.asString()) }

    when {
        state.isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.announcements.isEmpty() && state.error == null -> Text(
            text = stringResource(Res.string.announcement_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            val nowMillis = Clock.System.now().toEpochMilliseconds()
            state.announcements.forEachIndexed { index, announcement ->
                if (index > 0) HorizontalDivider()
                AnnouncementRow(
                    announcement = announcement,
                    expired = announcement.isExpired(nowMillis),
                    onEdit = { onAction(AnnouncementAdminAction.EditClick(announcement)) },
                    onDelete = { onAction(AnnouncementAdminAction.DeleteClick(announcement)) },
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onAction(AnnouncementAdminAction.Close) }) {
            Text(stringResource(Res.string.close))
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onAction(AnnouncementAdminAction.NewClick) }) {
            Text(stringResource(Res.string.announcement_new))
        }
    }
}

@Composable
private fun AnnouncementRow(
    announcement: Announcement,
    expired: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${announcement.id} ${announcement.german.title}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (expired) stringResource(Res.string.announcement_expired)
                else stringResource(Res.string.announcement_expires_at, millisToString(announcement.expiresAt, DATE_FORMAT)),
                style = MaterialTheme.typography.bodySmall,
                color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            announcement.creatorName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(Res.string.delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnouncementFormContent(
    form: AnnouncementForm,
    isSubmitting: Boolean,
    error: String?,
    onAction: (AnnouncementAdminAction) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val nowMillis = Clock.System.now().toEpochMilliseconds()

    Text(
        text = form.editingId?.let { stringResource(Res.string.announcement_edit_title, it.toString()) }
            ?: stringResource(Res.string.announcement_new),
        style = MaterialTheme.typography.titleLarge,
    )

    error?.let { ErrorText(it) }

    Text(
        text = stringResource(Res.string.announcement_language_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (form.editingId != null) {
        Text(
            text = stringResource(Res.string.announcement_edit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    ANNOUNCEMENT_LANGUAGES.forEach { language ->
        val text = form.textFor(language)
        val languageName = language.toUiText().asString()

        Text(
            text = if (language == LanguageSetting.GERMAN) stringResource(Res.string.announcement_required_language, languageName)
            else languageName,
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedTextField(
            value = text.title,
            onValueChange = { onAction(AnnouncementAdminAction.TitleChange(language, it)) },
            label = { Text(stringResource(Res.string.announcement_field_title)) },
            supportingText = { Text("${text.title.length}/${AnnouncementLimits.TITLE_MAX_LENGTH}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = text.body,
            onValueChange = { onAction(AnnouncementAdminAction.BodyChange(language, it)) },
            label = { Text(stringResource(Res.string.announcement_field_body)) },
            supportingText = { Text("${text.body.length}/${AnnouncementLimits.BODY_MAX_LENGTH}") },
            minLines = 2,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Text(
        text = "${stringResource(Res.string.announcement_expiry)}: ${millisToString(form.expiresAt, DATE_FORMAT)}",
        style = MaterialTheme.typography.titleSmall,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ANNOUNCEMENT_EXPIRY_PRESET_DAYS.forEach { days ->
            SuggestionChip(
                onClick = { onAction(AnnouncementAdminAction.ExpiryChange(nowMillis + days.days.inWholeMilliseconds)) },
                label = { Text(stringResource(Res.string.announcement_expiry_days, days.toString())) },
            )
        }
        SuggestionChip(
            onClick = { showDatePicker = true },
            label = { Text(stringResource(Res.string.announcement_expiry_custom)) },
        )
    }

    Text(
        text = stringResource(Res.string.announcement_preview),
        style = MaterialTheme.typography.titleSmall,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ANNOUNCEMENT_LANGUAGES.forEach { language ->
            FilterChip(
                selected = form.previewLanguage == language,
                onClick = { onAction(AnnouncementAdminAction.PreviewLanguageChange(language)) },
                label = { Text(language.toUiText().asString()) },
            )
        }
    }
    // Same look and fallbacks as the banner in the chat list
    val previewText = form.toPreview().textFor(form.previewLanguage)
    ChatSelectorDismissableInfo(
        title = previewText.title,
        description = previewText.body.ifBlank { null },
        onDismiss = {},
        icon = Icons.Default.Campaign,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onAction(AnnouncementAdminAction.CancelForm) }) {
            Text(stringResource(Res.string.cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(
            enabled = form.isValid(nowMillis) && !isSubmitting,
            onClick = { onAction(AnnouncementAdminAction.Submit) },
        ) {
            Text(stringResource(Res.string.save))
        }
    }

    if (showDatePicker) {
        ExpiryPickerDialog(
            initialMillis = form.expiresAt,
            nowMillis = nowMillis,
            onConfirm = { millis ->
                onAction(AnnouncementAdminAction.ExpiryChange(millis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun ExpiryPickerDialog(
    initialMillis: Long,
    nowMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeZone = TimeZone.currentSystemDefault()
    val minDateTime = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone)
    val maxDateTime = Instant.fromEpochMilliseconds(nowMillis + AnnouncementLimits.MAX_EXPIRY_DAYS.days.inWholeMilliseconds)
        .toLocalDateTime(timeZone)
    var selected by remember { mutableStateOf(Instant.fromEpochMilliseconds(initialMillis).toLocalDateTime(timeZone)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.toInstant(timeZone).toEpochMilliseconds()) }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            WheelDateTimePicker(
                modifier = Modifier.fillMaxWidth(),
                startDateTime = selected,
                minDateTime = minDateTime,
                maxDateTime = maxDateTime,
                rows = WheelRows.Count(3),
                textColor = MaterialTheme.colorScheme.onSurface,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ),
                onSnappedDateTime = { selected = it },
            )
        }
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
