package org.lerchenflo.schneaggchatv3mp.sharedUi.popups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.friend_request_accept
import schneaggchatv3mp.composeapp.generated.resources.send_friend_request
import schneaggchatv3mp.composeapp.generated.resources.unknown_user_request_received
import schneaggchatv3mp.composeapp.generated.resources.unknown_user_request_sent

/**
 * One popup for every user the own user is not friends with: picture (placeholder when none is
 * stored locally), name, and depending on [state] "Send request" / "Accept" or only "Close".
 * Accepting is the same call as sending a request, so both go to [onSendRequest].
 */
@Composable
fun UnknownUserDialog(
    name: String,
    profilePictureFilePath: String,
    state: FriendRequestState,
    onSendRequest: () -> Unit,
    onDismiss: () -> Unit,
    /** Only known in the user search. */
    commonFriendCount: Int? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ProfilePictureView(
                filepath = profilePictureFilePath,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
        },
        title = { Text(text = name, textAlign = TextAlign.Center) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                commonFriendCount?.let { Text(text = stringResource(Res.string.common_friends, it)) }
                when (state) {
                    FriendRequestState.NONE -> Unit
                    FriendRequestState.SENT -> Text(text = stringResource(Res.string.unknown_user_request_sent))
                    FriendRequestState.RECEIVED -> Text(text = stringResource(Res.string.unknown_user_request_received))
                }
            }
        },
        confirmButton = {
            when (state) {
                FriendRequestState.NONE -> TextButton(onClick = onSendRequest) {
                    Text(text = stringResource(Res.string.send_friend_request))
                }
                FriendRequestState.RECEIVED -> TextButton(onClick = onSendRequest) {
                    Text(text = stringResource(Res.string.friend_request_accept))
                }
                FriendRequestState.SENT -> TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.close))
                }
            }
        },
        dismissButton = if (state == FriendRequestState.SENT) null else {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        },
    )
}
