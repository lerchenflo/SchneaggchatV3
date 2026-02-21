package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
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

    //TODO: Nochboua vo do https://medium.com/design-bootcamp/whats-up-with-whatsapps-poll-ux-6dec6a630f2e (Whatsapp ui in besser)

    //TODO: MD Support für alle texte
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

            PollSmallInfoWindow(poll, myMessage)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        //Description
        poll.description?.let {
            Text(
                text = poll.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        poll.voteOptions.forEach { option ->
            PollMessageOptionView(
                option = option,
                multipleAnswers = poll.acceptsMultipleAnswers(), //Allow multiple answers if maxanswers is null(not set) or more than one
                votePercentage = option.voters.size.toFloat() / poll.getTotalVoteCount().toFloat(),
                myMessage = myMessage,
                voterIds = option.getVoterIdsForOption(),
                onOptionSelected = {
                    onAction(MessageAction.VotePoll(
                        messageId = message.id!!,
                        optionId = option.id,
                        checked = it
                    ))
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
                    text = "Eigene Antwort hinzufügen", // TODO: StringResource
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
                text = "Eigene Antwort hinzufügen", // TODO: StringResource
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = customOptionText,
                onValueChange = { customOptionText = it },
                label = { Text("Antwort") }, // TODO: StringResource
                placeholder = { Text("Deine Antwort eingeben...") }, // TODO: StringResource
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
                Text("Hinzufügen") // TODO: StringResource
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen") // TODO: StringResource
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
    voterIds: List<String?>,
    onOptionSelected: (Boolean) -> Unit
) {

    val optionCheckedByMe = option.voters.any { it.userId == SessionCache.getOwnIdValue() }


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
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$anonymousVoterCount",
                                fontSize = 6.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
@Composable
fun PollSmallInfoWindow(poll: PollMessage, myMessage: Boolean) {
    Box(
        modifier = Modifier
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
                    text = "${poll.getTotalVoteCount()} antworten", //TODO Stringressource
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (poll.getUniqueVoterCount() != 0) {
                Text(
                    text = "${poll.getUniqueVoterCount()} verschiedene user", //TODO Stringressource
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
            ) {
                // Multiple answers?
                if (poll.acceptsMultipleAnswers()) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Multiple answers",
                        modifier = Modifier.size(12.dp),
                        tint = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (poll.maxAnswers != null) {
                        Text(
                            text = poll.maxAnswers.toString(), //TODO Stringressource
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

                    if (poll.maxAllowedCustomAnswers != null) {
                        Text(
                            text = poll.maxAllowedCustomAnswers.toString(), //TODO Stringressource
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
            "Endet in: $formattedTime" // TODO: StringResource
        } else {
            "Beendet" // TODO: StringResource
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