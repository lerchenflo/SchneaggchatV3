package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction

@Composable
fun PollMessageContentView(
    message: Message,
    useMD: Boolean,
    onAction: (MessageAction) -> Unit = {}
){
    //TODO: Implement poll in message db + sync + senden

    //TODO: Nochboua vo do https://medium.com/design-bootcamp/whats-up-with-whatsapps-poll-ux-6dec6a630f2e (Whatsapp ui in besser)
    Column {

        //Title
        // Example usage when implementing poll voting UI:
        // Button(onClick = { onAction(MessageAction.VotePoll(message.id!!, option.id)) }) { ... }

    }
}