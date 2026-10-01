@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll

import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboAnnotationSource
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboInputField
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboText
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.rememberComboAnnotationSources
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.resolveComboAnnotationsToPlainText
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.millisToDuration
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.poll_add_custom_answer
import schneaggchatv3mp.composeapp.generated.resources.poll_anonymous_info
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_label
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_summary
import schneaggchatv3mp.composeapp.generated.resources.poll_answers_count
import schneaggchatv3mp.composeapp.generated.resources.poll_cannot_vote_on_unsent
import schneaggchatv3mp.composeapp.generated.resources.poll_closed
import schneaggchatv3mp.composeapp.generated.resources.poll_customoption_info
import schneaggchatv3mp.composeapp.generated.resources.poll_deleteoptions_info
import schneaggchatv3mp.composeapp.generated.resources.poll_ends_in
import schneaggchatv3mp.composeapp.generated.resources.poll_haslimitedentries_info
import schneaggchatv3mp.composeapp.generated.resources.poll_listmode_info
import schneaggchatv3mp.composeapp.generated.resources.poll_maxoptions_info
import schneaggchatv3mp.composeapp.generated.resources.poll_oneoption_info
import schneaggchatv3mp.composeapp.generated.resources.poll_option_claimed_count
import schneaggchatv3mp.composeapp.generated.resources.poll_option_delete
import schneaggchatv3mp.composeapp.generated.resources.poll_option_delete_confirm_text
import schneaggchatv3mp.composeapp.generated.resources.poll_option_delete_confirm_title
import schneaggchatv3mp.composeapp.generated.resources.poll_option_full
import schneaggchatv3mp.composeapp.generated.resources.poll_option_maxvoters_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_private_info
import schneaggchatv3mp.composeapp.generated.resources.poll_public_info
import schneaggchatv3mp.composeapp.generated.resources.poll_show_answers
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_pick_to_answer
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_option_count
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_expand
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_collapse
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_clear_confirm_title
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_clear_confirm_text
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_clear_confirm
import schneaggchatv3mp.composeapp.generated.resources.poll_tooltip_title
import schneaggchatv3mp.composeapp.generated.resources.poll_user_count
import schneaggchatv3mp.composeapp.generated.resources.poll_voter_you
import schneaggchatv3mp.composeapp.generated.resources.unknown_user
import schneaggchatv3mp.composeapp.generated.resources.unlimited
import kotlin.time.Clock

@Composable
fun PollMessageContentView(
    ownId: String,
    message: Message,
    useMD: Boolean,
    myMessage: Boolean,
    readerMap: Map<String, String>,
    onAction: (MessageAction) -> Unit = {}
){


    val poll = message.poll ?: run {
        Text(
            text = "Error: Poll data not available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }


    Column(
        modifier = Modifier.padding(4.dp)
    ) {
        PollLevelView(
            poll = poll,
            messageId = message.id,
            ownId = ownId,
            useMD = useMD,
            myMessage = myMessage,
            readerMap = readerMap,
            onAction = onAction,
            depth = 0,
            parentOptionId = null,
            lockedByOptionText = null,
        )
    }
}

/**
 * A vote that would throw away the user's answers in a sub poll, waiting for confirmation.
 * [subPollTitle] is the sub poll whose answers get lost.
 */
private data class PendingPollVote(
    val optionId: String,
    val checked: Boolean,
    val subPollTitle: String,
)

/**
 * One level of a poll: title, description, options (each possibly with an expandable sub poll, which
 * renders through this same composable) and the custom answer button.
 *
 * @param parentOptionId The option this level is the sub poll of, null for the root poll.
 * @param lockedByOptionText Non-null if the user can't answer this level yet because they did not pick
 *   the parent option (named by this text) - the level is then shown read-only.
 */
@Composable
private fun PollLevelView(
    poll: PollMessage,
    messageId: String?,
    ownId: String,
    useMD: Boolean,
    myMessage: Boolean,
    readerMap: Map<String, String>,
    onAction: (MessageAction) -> Unit,
    depth: Int,
    parentOptionId: String?,
    lockedByOptionText: String?,
) {
    val contentColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val canAnswer = lockedByOptionText == null

    Column {

        //Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComboText(
                text = poll.title,
                useMD = useMD,
                textColor = contentColor,
                style = if (depth == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))


            val tooltipState = rememberTooltipState(isPersistent = true)
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 12.dp
                ),
                tooltip = {

                    RichTooltip {
                        PollInfoTooltipContent(poll = poll)
                    }
                },
                state = tooltipState,
            ){
                PollSmallInfoWindow(
                    modifier = Modifier.clickable {
                        scope.launch {
                            if (tooltipState.isVisible) {
                                tooltipState.dismiss()
                            }else tooltipState.show()
                        }
                    },
                    poll = poll,
                    myMessage = myMessage
                )
            }
        }

        //Read-only sub poll: tell the user what unlocks it
        if (lockedByOptionText != null) {
            val annotationSources = rememberComboAnnotationSources()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(
                        Res.string.poll_subpoll_pick_to_answer,
                        resolveComboAnnotationsToPlainText(lockedByOptionText, annotationSources)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        //Description
        poll.description?.let {
            ComboText(
                text = poll.description,
                useMD = useMD,
                textColor = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        }


        val cannot_vote_on_unsent_message_text = stringResource(Res.string.poll_cannot_vote_on_unsent)

        fun sendVote(optionId: String, checked: Boolean) {
            if (messageId != null) {
                onAction(
                    MessageAction.VotePoll(
                        messageId = messageId,
                        optionId = optionId,
                        checked = checked
                    )
                )
            } else {
                SnackbarManager.showMessage(cannot_vote_on_unsent_message_text)
            }
        }

        var pendingVote by remember { mutableStateOf<PendingPollVote?>(null) }

        poll.voteOptions.forEach { option ->
            PollMessageOptionView(
                option = option,
                multipleAnswers = poll.acceptsMultipleAnswers(), //Allow multiple answers if maxanswers is null(not set) or more than one
                votePercentage = option.voters.size.toFloat() / poll.getTotalVoteCount().toFloat(),
                myMessage = myMessage,
                voterIds = option.getVoterIdsForOption(),
                full = poll.optionIsFull(option, ownId),
                onOptionSelected = { checked ->
                    //Unpicking an option - or picking one that pushes out the oldest vote on a
                    //limited poll - also drops the answers given in that option's sub poll
                    val optionLosingAnswers = if (checked) poll.optionDroppedBySelecting(ownId) else option
                    val lostSubPoll = optionLosingAnswers?.subPoll?.takeIf { it.hasUserVotedAnywhere(ownId) }

                    if (lostSubPoll != null) {
                        pendingVote = PendingPollVote(option.id, checked, lostSubPoll.title)
                    } else {
                        sendVote(option.id, checked)
                    }
                },
                ownId = ownId,
                useMD = useMD,
                showCheckbox = poll.showCheckboxes,
                enabled = canAnswer,
                //Fake option ids ("0", "1", ...) are used for the optimistic local echo until the server responds - deleting those would target nothing
                canDelete = poll.canDeleteOption(option, ownId) && messageId != null,
                onDelete = { onAction(MessageAction.DeletePollOption(messageId!!, option.id)) }
            )

            option.subPoll?.let { subPoll ->
                SubPollSection(
                    option = option,
                    subPoll = subPoll,
                    messageId = messageId,
                    ownId = ownId,
                    useMD = useMD,
                    myMessage = myMessage,
                    readerMap = readerMap,
                    onAction = onAction,
                    depth = depth,
                    parentLocked = !canAnswer,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        pendingVote?.let { vote ->
            val annotationSources = rememberComboAnnotationSources()
            AlertDialog(
                onDismissRequest = { pendingVote = null },
                title = { Text(stringResource(Res.string.poll_subpoll_clear_confirm_title)) },
                text = {
                    Text(
                        stringResource(
                            Res.string.poll_subpoll_clear_confirm_text,
                            resolveComboAnnotationsToPlainText(vote.subPollTitle, annotationSources)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingVote = null
                        sendVote(vote.optionId, vote.checked)
                    }) {
                        Text(stringResource(Res.string.poll_subpoll_clear_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingVote = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        //Add custom option - not while this (sub) poll is read-only
        if (poll.customAnswersEnabled && canAnswer) {
            var showDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .clickable { showDialog = true }
                    .border(
                        width = 1.dp,
                        color = if (myMessage) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom option",
                    modifier = Modifier.size(20.dp),
                    tint = if (myMessage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(Res.string.poll_add_custom_answer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (myMessage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            if (showDialog) {
                CustomPollOptionDialog(
                    showSlider = poll.voteOptions.any { it.maxVoters != null },
                    onDismiss = { showDialog = false },
                    onSubmit = { customOption ->
                        if (messageId != null) {
                            onAction(
                                MessageAction.AddCustomPollOption(
                                    messageId = messageId,
                                    text = customOption.text.text.trim(),
                                    maxAnswers = customOption.maxVoters,
                                    parentOptionId = parentOptionId
                                )
                            )
                        } else {
                            SnackbarManager.showMessage(cannot_vote_on_unsent_message_text)
                        }
                        showDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }


        //Results button only once, on the root poll - the dialog lists the sub polls as well.
        //A list-mode poll (no checkboxes) never accepts votes, so there is no results view to show
        if (depth == 0 && poll.showCheckboxes && (poll.visibility == PollVisibility.PUBLIC || (poll.visibility == PollVisibility.PRIVATE && poll.creatorId == ownId))) {
            var showVoterDialog by remember { mutableStateOf(false) }

            Row {
                Spacer(modifier = Modifier.weight(1f))

                NormalButton(
                    text = stringResource(Res.string.poll_show_answers),
                    onClick = {
                        showVoterDialog = true
                    },
                    primary = false,
                    showOutline = true
                )
                if (showVoterDialog) {

                    PollVoterOverviewDialog(
                        poll = poll,
                        onDismiss = { showVoterDialog = false },
                        readerMap = readerMap,
                        ownId = ownId
                    )
                }
            }
        }

    }
}

/**
 * Collapsible sub poll below its option. Opens by itself as soon as the user picks the option;
 * users who did not pick it can still expand it read-only (the server only sends it to them if the
 * creator made it visible to everyone).
 */
@Composable
private fun SubPollSection(
    option: PollVoteOption,
    subPoll: PollMessage,
    messageId: String?,
    ownId: String,
    useMD: Boolean,
    myMessage: Boolean,
    readerMap: Map<String, String>,
    onAction: (MessageAction) -> Unit,
    depth: Int,
    parentLocked: Boolean,
) {
    val pickedByMe = option.voters.any { it.userId == ownId }
    var expanded by rememberSaveable(option.id) { mutableStateOf(pickedByMe) }
    LaunchedEffect(pickedByMe) {
        if (pickedByMe) expanded = true
    }

    val accentColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val contentColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val annotationSources = rememberComboAnnotationSources()

    //Indented to line up with the option text next to the 24dp checkbox + 8dp gap
    Column(
        modifier = Modifier.padding(start = 32.dp, top = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.08f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accentColor
            )
            Text(
                text = resolveComboAnnotationsToPlainText(subPoll.title, annotationSources),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.poll_subpoll_option_count, subPoll.voteOptions.size.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) Res.string.poll_subpoll_collapse else Res.string.poll_subpoll_expand),
                modifier = Modifier.size(20.dp),
                tint = accentColor
            )
        }

        AnimatedVisibility(visible = expanded) {
            //Thin guide line on the left ties the nested poll to its option
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .drawBehind {
                        drawLine(
                            color = accentColor.copy(alpha = 0.5f),
                            start = Offset(1.dp.toPx(), 0f),
                            end = Offset(1.dp.toPx(), size.height),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    .padding(start = 10.dp)
            ) {
                PollLevelView(
                    poll = subPoll,
                    messageId = messageId,
                    ownId = ownId,
                    useMD = useMD,
                    myMessage = myMessage,
                    readerMap = readerMap,
                    onAction = onAction,
                    depth = depth + 1,
                    parentOptionId = option.id,
                    lockedByOptionText = if (parentLocked || !pickedByMe) option.text else null,
                )
            }
        }
    }
}

@Composable
fun PollVoterOverviewDialog(
    poll: PollMessage,
    readerMap: Map<String, String>,
    ownId: String,
    onDismiss: () -> Unit
) {
    val pictureManager = koinInject<PictureManager>()
    val annotationSources = rememberComboAnnotationSources()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = resolveComboAnnotationsToPlainText(poll.title, annotationSources),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PollVoterOverviewEntries(
                    poll = poll,
                    readerMap = readerMap,
                    ownId = ownId,
                    pictureManager = pictureManager,
                    annotationSources = annotationSources,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Voters per option of one poll level. Sub polls with at least one answer follow right below their
 * option, indented, through this same composable.
 */
@Composable
private fun PollVoterOverviewEntries(
    poll: PollMessage,
    readerMap: Map<String, String>,
    ownId: String,
    pictureManager: PictureManager,
    annotationSources: List<ComboAnnotationSource>,
) {
    poll.voteOptions.forEach { option ->
        val answeredSubPoll = option.subPoll?.takeIf { it.hasAnyVotes() }
        if (option.voters.isEmpty() && answeredSubPoll == null) return@forEach

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = resolveComboAnnotationsToPlainText(option.text, annotationSources),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            option.voters.forEach { voter ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (voter.userId != null) {
                        ProfilePictureView(
                            filepath = pictureManager.getProfilePicFilePath(voter.userId, false),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (voter.userId == ownId) {
                                stringResource(Res.string.poll_voter_you)
                            }else {
                                readerMap[voter.userId] ?: stringResource(Res.string.unknown_user)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Blind,
                            contentDescription = "Anonymous",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            answeredSubPoll?.let { subPoll ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = resolveComboAnnotationsToPlainText(subPoll.title, annotationSources),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    PollVoterOverviewEntries(
                        poll = subPoll,
                        readerMap = readerMap,
                        ownId = ownId,
                        pictureManager = pictureManager,
                        annotationSources = annotationSources,
                    )
                }
            }

            HorizontalDivider()
        }
    }
}

@Composable
fun CustomPollOptionDialog(
    showSlider: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (PollOptionInput) -> Unit
) {

    var option by remember { mutableStateOf(PollOptionInput()) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.poll_add_custom_answer),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                ComboInputField(
                    value = option.text,
                    onValueChange = { option.text = it },
                    label = { Text(stringResource(Res.string.poll_answer_label)) },
                    placeholder = { Text(stringResource(Res.string.poll_answer_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showSlider) {
                    val sliderValue = option.maxVoters ?: 10

                    Text(
                        text = stringResource(
                            Res.string.poll_option_maxvoters_withcount,
                            if (sliderValue in 1..9) sliderValue.toString() else "∞"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Slider(
                        value = sliderValue.toFloat(),
                        onValueChange = { option.maxVoters = it.toInt().let { count -> if (count == 10) null else count } },
                        valueRange = 1f..10f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (option.text.text.isNotBlank()) {
                        onSubmit(option)
                    }
                },
                enabled = option.text.text.isNotBlank()
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
fun PollMessageOptionView(
    ownId: String,
    option: PollVoteOption,
    multipleAnswers: Boolean,
    votePercentage: Float,
    myMessage: Boolean,
    voterIds: List<String?>,
    onOptionSelected: (Boolean) -> Unit,
    full: Boolean = false,
    useMD: Boolean = false,
    showCheckbox: Boolean = true,
    enabled: Boolean = true, //False on a read-only sub poll: results visible, voting off
    canDelete: Boolean = false,
    onDelete: () -> Unit = {}
) {

    val optionCheckedByMe = option.voters.any { it.userId == ownId }
    val selectable = enabled && showCheckbox && (optionCheckedByMe || !full)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clickable(enabled = selectable) { onOptionSelected(!optionCheckedByMe) },
        verticalAlignment = Alignment.CenterVertically
    ) {

        //Start checkbox / radiobutton - a list-mode poll (showCheckbox == false) skips this entirely, it's just text rows
        if (showCheckbox) {
            if (multipleAnswers) {
                Checkbox(
                    checked = optionCheckedByMe,
                    onCheckedChange = { onOptionSelected(!optionCheckedByMe) },
                    enabled = selectable,
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        uncheckedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = if (myMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                    )
                )
            } else {
                RadioButton(
                    selected = optionCheckedByMe,
                    onClick = { onOptionSelected(!optionCheckedByMe) },
                    enabled = selectable,
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        unselectedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        //Userview, text + progressbar
        Column(
            horizontalAlignment = Alignment.Start,
        ) {

            //Row for text + userview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                val locModifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp)
                    .clipToBounds()

                if (option.custom) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = locModifier
                    ) {

                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Custom user answer",
                            modifier = Modifier.size(24.dp),
                            tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ComboText(
                            text = option.text,
                            useMD = useMD,
                            textColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    ComboText(
                        text = option.text,
                        useMD = useMD,
                        textColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = locModifier
                    )
                }

                //Voting-only info - meaningless on a plain list, so all hidden when showCheckbox is false
                if (showCheckbox) {
                    // Per-entry vote limit (claimed/max, or "full")
                    option.maxVoters?.let { maxVoters ->
                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = if (full) {
                                stringResource(Res.string.poll_option_full)
                            } else {
                                stringResource(Res.string.poll_option_claimed_count, option.voters.size.toString(), maxVoters.toString())
                            },
                            fontSize = 10.sp,
                            color = if (full) {
                                MaterialTheme.colorScheme.error
                            } else if (myMessage) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }



                    Spacer(modifier = Modifier.width(4.dp))

                    //No Koin in @Previews - avatars are skipped there, the anonymous "+n" count still shows
                    val pictureManager = if (LocalInspectionMode.current) null else koinInject<PictureManager>()


                    val nonNullVoterIds = voterIds.filterNotNull()
                    val anonymousVoterCount = voterIds.count { it == null }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {

                        // Show profile pictures for identified voters
                        if (pictureManager != null) nonNullVoterIds.forEach { userId ->
                            ProfilePictureView(
                                filepath = pictureManager.getProfilePicFilePath(userId, false),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Show count of anonymous voters if any
                        if (anonymousVoterCount > 0) {
                            Text(
                                text = "+$anonymousVoterCount",
                                fontSize = 12.sp,
                                color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (canDelete) {
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            //Skip the confirm dialog when nobody voted - nothing is lost
                            if (option.voters.isEmpty()) onDelete() else showDeleteConfirm = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.poll_option_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (showCheckbox) {
                Spacer(modifier = Modifier.height(2.dp))

                LinearProgressIndicator(
                    progress = { votePercentage },
                    drawStopIndicator = {}, //Remove stop indicator
                    trackColor = Color.Transparent,
                    color = if (myMessage) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }

        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.poll_option_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.poll_option_delete_confirm_text, option.text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(stringResource(Res.string.poll_option_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}


/**
 * Small box which shows what the poll can do (Multiple answers, custom answers, current answer count, users current answer count)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollSmallInfoWindow(
    modifier: Modifier,
    poll: PollMessage,
    myMessage: Boolean) {

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {

            if (poll.getTotalVoteCount() != 0) {
                Text(
                    text = stringResource(Res.string.poll_answers_count, poll.getTotalVoteCount()),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (poll.getUniqueVoterCount() != 0) {
                Text(
                    text = stringResource(Res.string.poll_user_count, poll.getUniqueVoterCount()),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Answer type - meaningless on a list-mode poll (never votes), show a list icon instead
                if (!poll.showCheckboxes) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "List",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (poll.acceptsMultipleAnswers()) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Multiple answers",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (poll.maxAnswers != null && poll.maxAnswers != 10) {
                        Text(
                            text = poll.maxAnswers.toString(),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Single answers",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Custom answers
                if (poll.customAnswersEnabled) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Custom answers",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (poll.maxAllowedCustomAnswers != null && poll.maxAllowedCustomAnswers != 10) {
                        Text(
                            text = poll.maxAllowedCustomAnswers.toString(),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                //Visibility
                when (poll.visibility) {
                    PollVisibility.PUBLIC -> {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Public",
                            modifier = Modifier.size(12.dp),
                            tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PollVisibility.PRIVATE -> {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = "Private",
                            modifier = Modifier.size(12.dp),
                            tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PollVisibility.ANONYMOUS -> {
                        Icon(
                            imageVector = Icons.Default.Blind,
                            contentDescription = "Anonymous",
                            modifier = Modifier.size(12.dp),
                            tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Per-entry vote limits
                if (poll.voteOptions.any { it.maxVoters != null }) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Limited entries",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Options can be deleted
                if (poll.allowDeleteOptions) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Options can be deleted",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            poll.expiresAt?.let {
                PollCountdownTimer(expiresAt = it, myMessage = myMessage)
            }
        }
    }
}

@Composable
fun PollInfoTooltipContent(poll: PollMessage) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.poll_tooltip_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Answer type - a list-mode poll never votes, so show that instead
        if (!poll.showCheckboxes) {
            TooltipRow(
                icon = Icons.AutoMirrored.Filled.List,
                text = stringResource(Res.string.poll_listmode_info)
            )
        } else {
            TooltipRow(
                icon = if (poll.acceptsMultipleAnswers()) Icons.Default.CheckBox else Icons.Default.CheckCircle,
                text = if (poll.acceptsMultipleAnswers()) {
                    stringResource(Res.string.poll_maxoptions_info, poll.maxAnswers ?: stringResource(Res.string.unlimited))
                } else {
                    stringResource(Res.string.poll_oneoption_info)
                }
            )
        }

        // Custom answers
        if (poll.customAnswersEnabled) {
            TooltipRow(
                icon = Icons.Default.Add,
                text = stringResource(Res.string.poll_customoption_info, poll.maxAllowedCustomAnswers ?: stringResource(Res.string.unlimited))
            )
        }

        // Visibility
        TooltipRow(
            icon = when (poll.visibility) {
                PollVisibility.PUBLIC -> Icons.Default.Public
                PollVisibility.PRIVATE -> Icons.Default.PersonSearch
                PollVisibility.ANONYMOUS -> Icons.Default.Blind
            },
            text = when (poll.visibility) {
                PollVisibility.PUBLIC -> stringResource(Res.string.poll_public_info)
                PollVisibility.PRIVATE -> stringResource(Res.string.poll_private_info)
                PollVisibility.ANONYMOUS -> stringResource(Res.string.poll_anonymous_info)
            }
        )

        // Per-entry vote limits
        if (poll.voteOptions.any { it.maxVoters != null }) {
            TooltipRow(
                icon = Icons.Default.Lock,
                text = stringResource(Res.string.poll_haslimitedentries_info)
            )
        }

        // Options can be deleted
        if (poll.allowDeleteOptions) {
            TooltipRow(
                icon = Icons.Default.Delete,
                text = stringResource(Res.string.poll_deleteoptions_info)
            )
        }

        // Stats if available
        if (poll.getTotalVoteCount() > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(Res.string.poll_answer_summary, poll.getTotalVoteCount(), poll.getUniqueVoterCount()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TooltipRow(
    icon: ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


@Composable
fun PollCountdownTimer(expiresAt: Long, myMessage: Boolean) {
    var timeRemaining by remember { mutableStateOf(calculateTimeRemaining(expiresAt)) }

    LaunchedEffect(expiresAt) {
        while (timeRemaining > 0) {
            delay(1000) // Update every second
            timeRemaining = calculateTimeRemaining(expiresAt)
        }
    }

    val formattedTime ="${millisToDuration(timeRemaining)})"

    Text(
        text = if (timeRemaining > 0) {
            stringResource(Res.string.poll_ends_in, formattedTime)
        } else {
            stringResource(Res.string.poll_closed)
        },
        fontSize = 10.sp,
        color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun calculateTimeRemaining(expiresAt: Long): Long {
    return maxOf(0, expiresAt - Clock.System.now().toEpochMilliseconds())
}

