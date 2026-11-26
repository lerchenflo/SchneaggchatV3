package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.sharedUi.text.text
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.FeedbackAction
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.FeedbackDetailState
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutEditButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ChangeStringDialog
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.feedback_change_status
import schneaggchatv3mp.composeapp.generated.resources.feedback_comment_hint
import schneaggchatv3mp.composeapp.generated.resources.feedback_comments
import schneaggchatv3mp.composeapp.generated.resources.feedback_delete_comment_confirm
import schneaggchatv3mp.composeapp.generated.resources.feedback_delete_entry
import schneaggchatv3mp.composeapp.generated.resources.feedback_delete_entry_confirm
import schneaggchatv3mp.composeapp.generated.resources.feedback_details_load_error
import schneaggchatv3mp.composeapp.generated.resources.feedback_edit_comment
import schneaggchatv3mp.composeapp.generated.resources.feedback_edit_entry
import schneaggchatv3mp.composeapp.generated.resources.feedback_edited
import schneaggchatv3mp.composeapp.generated.resources.feedback_no_comments
import schneaggchatv3mp.composeapp.generated.resources.feedback_retry
import schneaggchatv3mp.composeapp.generated.resources.feedback_seeded_author
import schneaggchatv3mp.composeapp.generated.resources.feedback_send_comment
import schneaggchatv3mp.composeapp.generated.resources.feedback_where_to_find
import schneaggchatv3mp.composeapp.generated.resources.feedback_write_comment

internal const val DATE_FORMAT = "dd.MM.yyyy"

/**
 * Full view of one entry with all comments. A plain Dialog rather than a ModalBottomSheet, for the
 * same reason as the event popups: the sheet's drag handling can swallow input to the text field.
 */
@Composable
fun FeedbackDetailDialog(
    detailState: FeedbackDetailState,
    viewerIsAdmin: Boolean,
    /** Names of the entry creator and the comment authors, by user id. */
    userNames: Map<String, UserIdentityName>,
    onAction: (FeedbackAction) -> Unit,
) {
    val entry = detailState.entry
    val detail = detailState.detail
    val editedLabel = stringResource(Res.string.feedback_edited)

    // Deletes always go through a confirmation; null = no confirmation open
    var confirmDeleteEntry by remember { mutableStateOf(false) }
    var confirmDeleteCommentId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { onAction(FeedbackAction.OnDetailDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item(key = "header") {
                    DetailHeader(
                        entry = entry,
                        viewerIsAdmin = viewerIsAdmin,
                        onStatusChange = { onAction(FeedbackAction.OnStatusChange(entry.id, it)) },
                        // The form needs the loaded details (the location is only in there)
                        onEditClick = if (entry.canEdit && detail != null) {
                            { onAction(FeedbackAction.OnEditEntryClick) }
                        } else null,
                        onDeleteClick = { confirmDeleteEntry = true },
                        onClose = { onAction(FeedbackAction.OnDetailDismiss) },
                    )
                }

                item(key = "body") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = listOfNotNull(
                                authorName(entry, userNames),
                                millisToString(entry.createdAt, DATE_FORMAT),
                                deviceInfo(detail?.platform, detail?.appVersion),
                                editedLabel.takeIf { entry.editedAt != null },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = detail?.description ?: entry.description,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        detail?.location?.let { location ->
                            Text(
                                text = stringResource(Res.string.feedback_where_to_find, location),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        FeedbackTagRow(entry.tags, isPremium = entry.isPremium)
                        FeedbackVoteRow(
                            entry = entry,
                            onVoteClick = { onAction(FeedbackAction.OnVoteClick(entry.id, it)) }
                        )
                        HorizontalDivider()
                        Text(
                            text = stringResource(Res.string.feedback_comments),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                when {
                    detailState.isLoading -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }

                    detailState.loadFailed -> item(key = "error") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(Res.string.feedback_details_load_error),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { onAction(FeedbackAction.OnDetailRetryClick) }) {
                                Text(stringResource(Res.string.feedback_retry))
                            }
                        }
                    }

                    detail != null && detail.comments.isEmpty() -> item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.feedback_no_comments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    detail != null -> items(detail.comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            authorName = userNames[comment.authorId].text(),
                            editedLabel = editedLabel,
                            showDeviceInfo = entry.type == FeedbackType.BUG,
                            onEditClick = { onAction(FeedbackAction.OnEditCommentClick(comment.id)) },
                            onDeleteClick = { confirmDeleteCommentId = comment.id },
                        )
                    }
                }
            }

            // Comments can be written once the details are loaded
            if (detail != null) {
                WriteCommentBar(
                    isSending = detailState.isSendingComment,
                    onClick = { onAction(FeedbackAction.OnWriteCommentClick) },
                )
            }
        }
    }

    if (detailState.isCommentPopupOpen) {
        CommentPopup(
            draft = detailState.commentDraft,
            isEditing = detailState.editingCommentId != null,
            onAction = onAction,
        )
    }

    if (confirmDeleteEntry) {
        ConfirmDeleteDialog(
            text = stringResource(Res.string.feedback_delete_entry_confirm),
            onConfirm = {
                confirmDeleteEntry = false
                onAction(FeedbackAction.OnDeleteEntry(entry.id))
            },
            onDismiss = { confirmDeleteEntry = false },
        )
    }

    confirmDeleteCommentId?.let { commentId ->
        ConfirmDeleteDialog(
            text = stringResource(Res.string.feedback_delete_comment_confirm),
            onConfirm = {
                confirmDeleteCommentId = null
                onAction(FeedbackAction.OnDeleteComment(commentId))
            },
            onDismiss = { confirmDeleteCommentId = null },
        )
    }
}

@Composable
private fun DetailHeader(
    entry: FeedbackEntry,
    viewerIsAdmin: Boolean,
    onStatusChange: (FeedbackStatus) -> Unit,
    /** Null hides the edit button. */
    onEditClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (viewerIsAdmin) {
            // Admins change the status straight from the pill
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { menuOpen = true }
                ) {
                    FeedbackStatusPill(entry.status)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(Res.string.feedback_change_status),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    FeedbackStatus.forType(entry.type).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(stringResource(status.labelRes())) },
                            onClick = {
                                menuOpen = false
                                if (status != entry.status) onStatusChange(status)
                            }
                        )
                    }
                }
            }
        } else {
            FeedbackStatusPill(entry.status)
        }

        Box(Modifier.weight(1f))

        if (onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.feedback_edit_entry),
                )
            }
        }
        if (entry.canDelete) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.feedback_delete_entry),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.close))
        }
    }
}

@Composable
private fun CommentItem(
    comment: FeedbackComment,
    authorName: String,
    editedLabel: String,
    showDeviceInfo: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val meta = listOfNotNull(
        authorName,
        millisToString(comment.createdAt, DATE_FORMAT),
        if (showDeviceInfo) deviceInfo(comment.platform, comment.appVersion) else null,
        editedLabel.takeIf { comment.editedAt != null },
    ).joinToString(" · ")

    val footer: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = if (comment.isDevComment) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (comment.canEdit) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.feedback_edit_comment),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (comment.canDelete) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(Res.string.delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (comment.isDevComment) {
        DevCommentBlock(text = comment.text, footer = footer)
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
            footer()
        }
    }
}

/** Bar under the comments: opens the comment popup, a spinner while a comment is being sent. */
@Composable
private fun WriteCommentBar(isSending: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSending) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp).size(24.dp), strokeWidth = 2.dp)
        } else {
            AboutEditButton(
                text = stringResource(Res.string.feedback_write_comment),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Popup above the detail dialog for a new comment or an edit of an own one. Its own small dialog
 * stays above the keyboard; a text field inside the tall detail dialog made it wobble while typing.
 * Every change goes to the draft, so a dismissed new comment is still there when it opens again.
 */
@Composable
private fun CommentPopup(
    draft: String,
    isEditing: Boolean,
    onAction: (FeedbackAction) -> Unit,
) {
    ChangeStringDialog(
        title = stringResource(if (isEditing) Res.string.feedback_edit_comment else Res.string.feedback_write_comment),
        oldString = draft,
        placeholder = stringResource(Res.string.feedback_comment_hint),
        // An edit keeps the default "Change" label
        confirmText = if (isEditing) null else stringResource(Res.string.feedback_send_comment),
        maxLength = COMMENT_MAX_LENGTH,
        onValueChange = { onAction(FeedbackAction.OnCommentDraftChange(it)) },
        onDismiss = { onAction(FeedbackAction.OnCommentPopupDismiss) },
        // The draft already holds the text (onValueChange), sending reads it from there
        updateString = { onAction(FeedbackAction.OnSendCommentClick) },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

/** Creator shown on the board: the team for seeded entries, else the creator's name from [userNames]. */
@Composable
internal fun authorName(entry: FeedbackEntry, userNames: Map<String, UserIdentityName>): String = when {
    entry.isSeeded -> stringResource(Res.string.feedback_seeded_author)
    else -> entry.creatorId?.let { userNames[it] }.text()
}

private fun deviceInfo(platform: String?, appVersion: String?): String? =
    listOfNotNull(platform, appVersion?.let { "v$it" }).joinToString(" ").takeIf { it.isNotEmpty() }

private const val COMMENT_MAX_LENGTH = 1000
