package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
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

    }
}

@Composable
fun PollMessageOptionView(
    option: PollVoteOption,
    multipleAnswers: Boolean,
    votePercentage: Float,
    myMessage: Boolean,
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
            modifier = Modifier.height(24.dp)
        ) {

            Row {
                Text(
                    text = option.text,
                    color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                //TODO: Voted user profile pics??
            }

            LinearProgressIndicator(
                progress = { votePercentage },
                drawStopIndicator = {}, //Remove stop indicator
                modifier = Modifier.height(16.dp)
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
                }
            }

            //Expires at (TODO)
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