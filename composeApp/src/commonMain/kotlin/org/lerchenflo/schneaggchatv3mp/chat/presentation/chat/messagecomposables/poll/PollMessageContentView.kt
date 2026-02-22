@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RichTooltip
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.OptionMetadata
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollUiState
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.poll_add_custom_answer
import schneaggchatv3mp.composeapp.generated.resources.poll_anonymous_info
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_label
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_answer_summary
import schneaggchatv3mp.composeapp.generated.resources.poll_answers_count
import schneaggchatv3mp.composeapp.generated.resources.poll_closed
import schneaggchatv3mp.composeapp.generated.resources.poll_customoption_info
import schneaggchatv3mp.composeapp.generated.resources.poll_ends_in
import schneaggchatv3mp.composeapp.generated.resources.poll_maxoptions_info
import schneaggchatv3mp.composeapp.generated.resources.poll_oneoption_info
import schneaggchatv3mp.composeapp.generated.resources.poll_private_info
import schneaggchatv3mp.composeapp.generated.resources.poll_public_info
import schneaggchatv3mp.composeapp.generated.resources.poll_tooltip_title
import schneaggchatv3mp.composeapp.generated.resources.poll_user_count
import schneaggchatv3mp.composeapp.generated.resources.unlimited
import kotlin.time.Clock

@Composable
fun PollMessageContentView(
    message: Message,
    useMD: Boolean,
    onAction: (MessageAction) -> Unit = {}
){

    val myMessage = message.myMessage

    val poll = message.poll ?: run {
        Text(
            text = "Error: Poll data not available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    // in da Theorie sött des Poll stats im hintergrund lada und damit da chat schneller macha. ob des in da praxis tatsächlich so isch woas i ned
    val pollUiState by produceState(initialValue = PollUiState(), key1 = message.poll) {
        // Run heavy calculations on the Default (Background) dispatcher
        withContext(kotlinx.coroutines.Dispatchers.Default) {
            val totalVotes = poll.getTotalVoteCount()
            val uniqueVoters = poll.getUniqueVoterCount()
            val ownId = SessionCache.getOwnIdValue()

            val metadataMap = poll.voteOptions.associate { option ->
                option.id to OptionMetadata(
                    votePercentage = if (totalVotes > 0) option.voters.size.toFloat() / totalVotes.toFloat() else 0f,
                    isCheckedByMe = option.voters.any { it.userId == ownId },
                    voterIds = option.getVoterIdsForOption()
                )
            }

            value = PollUiState(
                totalVotes = totalVotes,
                uniqueVoterCount = uniqueVoters,
                optionsMetadata = metadataMap
            )
        }
    }


    //TODO: MD Support für alle texte??
    Column(
        modifier = Modifier.padding(4.dp)
    ) {

        // Example usage when implementing poll voting UI:
        // Button(onClick = { onAction(MessageAction.VotePoll(message.id!!, option.id)) }) { ... }

        //Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = poll.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    uiState = pollUiState,
                    myMessage = myMessage
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        //Description
        poll.description?.let {
            Text(
                text = poll.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        }


        poll.voteOptions.forEach { option ->
            val meta = pollUiState.optionsMetadata[option.id]
            PollMessageOptionView(
                option = option,
                multipleAnswers = poll.acceptsMultipleAnswers(),
                // Use pre-calculated values
                votePercentage = meta?.votePercentage ?: 0f,
                optionCheckedByMe = meta?.isCheckedByMe ?: false,
                voterIds = meta?.voterIds ?: emptyList(),
                myMessage = myMessage,
                onOptionSelected = { checked ->
                    onAction(MessageAction.VotePoll(message.id!!, option.id, checked))
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        //Add custom option
        if (poll.customAnswersEnabled) {
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
                    onDismiss = { showDialog = false },
                    onSubmit = { customOption ->
                        onAction(MessageAction.AddCustomPollOption(message.id!!, customOption))
                        showDialog = false
                    }
                )
            }
        }

    }
}

@Composable
fun CustomPollOptionDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var customOptionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.poll_add_custom_answer),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = customOptionText,
                onValueChange = { customOptionText = it },
                label = { Text(stringResource(Res.string.poll_answer_label)) },
                placeholder = { Text(stringResource(Res.string.poll_answer_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (customOptionText.isNotBlank()) {
                        onSubmit(customOptionText.trim())
                    }
                },
                enabled = customOptionText.isNotBlank()
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
    option: PollVoteOption,
    multipleAnswers: Boolean,
    votePercentage: Float,
    myMessage: Boolean,
    optionCheckedByMe: Boolean,
    voterIds: List<String?>,
    onOptionSelected: (Boolean) -> Unit
) {

    // val optionCheckedByMe = option.voters.any { it.userId == SessionCache.getOwnIdValue() }


    Row(
        modifier = Modifier.clickable {
            onOptionSelected(!optionCheckedByMe)
        }
    ) {

        //Start checkbox / radiobutton
        if (multipleAnswers) {
            Checkbox(
                checked = optionCheckedByMe,
                onCheckedChange = { onOptionSelected(!optionCheckedByMe) },
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
                modifier = Modifier.size(24.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    unselectedColor = if (myMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))


        Column(
            horizontalAlignment = Alignment.Start,
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {


                if (option.custom) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Custom user answer",
                            modifier = Modifier.size(12.dp),
                            tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = option.text,
                            color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = option.text,
                        color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                val pictureManager = koinInject<PictureManager>()


                val nonNullVoterIds = voterIds.filterNotNull()
                val anonymousVoterCount = voterIds.count { it == null }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show profile pictures for identified voters
                    nonNullVoterIds.forEach { userId ->
                        ProfilePictureView(
                            filepath = pictureManager.getProfilePicFilePath(userId, false),
                            modifier = Modifier.size(14.dp)
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

            LinearProgressIndicator(
                progress = { votePercentage },
                drawStopIndicator = {}, //Remove stop indicator
                trackColor = Color.Transparent,
                color = if (myMessage) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )

        }
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
    uiState: PollUiState,
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

            if (uiState.totalVotes != 0) {
                Text(
                    text = stringResource(Res.string.poll_answers_count, uiState.totalVotes),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.uniqueVoterCount != 0) {
                Text(
                    text = stringResource(Res.string.poll_user_count, uiState.uniqueVoterCount),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Multiple answers?
                if (poll.acceptsMultipleAnswers()) {
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
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Answer type
        TooltipRow(
            icon = if (poll.acceptsMultipleAnswers()) Icons.Default.CheckBox else Icons.Default.CheckCircle,
            text = if (poll.acceptsMultipleAnswers()) {
                stringResource(Res.string.poll_maxoptions_info, poll.maxAnswers ?: stringResource(Res.string.unlimited))
            } else {
                stringResource(Res.string.poll_oneoption_info)
            }
        )

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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

    val formattedTime = formatTimeRemaining(timeRemaining)

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

private fun formatTimeRemaining(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}