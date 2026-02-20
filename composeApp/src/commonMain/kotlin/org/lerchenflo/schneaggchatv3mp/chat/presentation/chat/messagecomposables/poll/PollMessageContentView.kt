package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.sqlite.throwSQLiteException
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction

@Composable
fun PollMessageContentView(
    message: Message,
    useMD: Boolean,
    onAction: (MessageAction) -> Unit = {}
){

    require(message.msgType == MessageType.POLL) {"Message is not a pollmessage"}

    println("Poll composable: $message")
    val poll = message.poll ?: run {
        println("Error: Poll message has null poll data. Message: $message")
        Text(
            text = "Error: Poll data not available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    //TODO: Nochboua vo do https://medium.com/design-bootcamp/whats-up-with-whatsapps-poll-ux-6dec6a630f2e (Whatsapp ui in besser)

    //TODO: MD Support für alle texte
    Column {

        // Example usage when implementing poll voting UI:
        // Button(onClick = { onAction(MessageAction.VotePoll(message.id!!, option.id)) }) { ... }

        //Title
        Text(
            text = poll.title,
            style = MaterialTheme.typography.titleLarge
        )

        HorizontalDivider()

        //Description
        poll.description?.let {
            Text(
                text = poll.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        //Expires at (TODO)
        /*
        poll.expiresAt?.let {
            Text(
                text = ""
            )
        }

         */

        poll.voteOptions.forEach {
            Text(
                it.text
            )
        }

    }
}