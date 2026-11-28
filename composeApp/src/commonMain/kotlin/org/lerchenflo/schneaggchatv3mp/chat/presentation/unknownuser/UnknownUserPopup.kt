package org.lerchenflo.schneaggchatv3mp.chat.presentation.unknownuser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ZoomableProfilePicture
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.rememberInsetProfilePicturePlaceholder
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.text
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.call
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.friend_request_accept
import schneaggchatv3mp.composeapp.generated.resources.send_friend_request
import schneaggchatv3mp.composeapp.generated.resources.unknown_user_request_received
import schneaggchatv3mp.composeapp.generated.resources.unknown_user_request_sent
import schneaggchatv3mp.composeapp.generated.resources.user_popup_info
import schneaggchatv3mp.composeapp.generated.resources.user_popup_message

/** Share of the avatar the placeholder figure stays away from each edge. */
private const val PLACEHOLDER_INSET = 0.15f

@Composable
fun UnknownUserPopupRoot(route: Route.UnknownUser) {
    val viewModel = koinViewModel<UnknownUserViewModel> { parametersOf(route) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    UnknownUserPopup(
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * Card in the middle of the screen (the dialog scene dims the screen behind it): picture, name and
 * the friend request actions for the current [UnknownUserState.requestState].
 */
@Composable
fun UnknownUserPopup(
    state: UnknownUserState,
    onAction: (UnknownUserAction) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .widthIn(max = 400.dp)
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 16.dp)
        ) {
            UnknownUserPicture(filePath = state.profilePictureFilePath)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = state.name.text(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            state.commonFriendCount?.let { count ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.common_friends, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            requestStateText(state.requestState)?.let { text ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isFriend) {
                FriendActions(phoneNumber = state.phoneNumber, onAction = onAction)
            } else {
                PopupActions(requestState = state.requestState, onAction = onAction)
            }
        }
    }
}

/** The ringed avatar; a tap opens the picture big. Most users here have no picture, only the placeholder. */
@Composable
private fun UnknownUserPicture(filePath: String) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
    ) {
        // Keep the whole placeholder figure inside the ring
        ZoomableProfilePicture(
            filepath = filePath,
            contentDescription = null,
            placeholder = rememberInsetProfilePicturePlaceholder(insetFraction = PLACEHOLDER_INSET),
            modifier = Modifier.size(104.dp),
        )
    }
}

@Composable
private fun requestStateText(requestState: FriendRequestState?): String? = when (requestState) {
    FriendRequestState.SENT -> stringResource(Res.string.unknown_user_request_sent)
    FriendRequestState.RECEIVED -> stringResource(Res.string.unknown_user_request_received)
    FriendRequestState.NONE, null -> null
}

/** A friend's row like WhatsApp's: Message, Call (only with a shared phone number), Info. */
@Composable
private fun FriendActions(
    phoneNumber: String?,
    onAction: (UnknownUserAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        FriendActionButton(
            icon = Icons.AutoMirrored.Outlined.Chat,
            label = stringResource(Res.string.user_popup_message),
            onClick = { onAction(UnknownUserAction.OnMessageClick) }
        )
        if (phoneNumber != null) {
            FriendActionButton(
                icon = Icons.Outlined.Call,
                label = stringResource(Res.string.call),
                onClick = { onAction(UnknownUserAction.OnCallClick) }
            )
        }
        FriendActionButton(
            icon = Icons.Outlined.Info,
            label = stringResource(Res.string.user_popup_info),
            onClick = { onAction(UnknownUserAction.OnInfoClick) }
        )
    }
}

@Composable
private fun FriendActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Full-width buttons: the main action on top, closing below it; a spinner while loading. */
@Composable
private fun PopupActions(
    requestState: FriendRequestState?,
    onAction: (UnknownUserAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val confirmLabel = when (requestState) {
            null -> {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                return@Column
            }
            FriendRequestState.NONE -> stringResource(Res.string.send_friend_request)
            FriendRequestState.RECEIVED -> stringResource(Res.string.friend_request_accept)
            // Nothing to do but wait for the answer
            FriendRequestState.SENT -> null
        }

        confirmLabel?.let { label ->
            Button(
                onClick = { onAction(UnknownUserAction.OnSendRequestClick) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = label)
            }
        }

        TextButton(
            onClick = { onAction(UnknownUserAction.OnDismissClick) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    if (requestState == FriendRequestState.SENT) Res.string.close else Res.string.cancel
                )
            )
        }
    }
}

@Preview
@Composable
private fun UnknownUserPopupPreview() {
    SchneaggchatTheme {
        UnknownUserPopup(
            state = UnknownUserState(
                name = UserIdentityName.Known("Max Muster"),
                commonFriendCount = 3,
                requestState = FriendRequestState.NONE,
            ),
            onAction = {},
        )
    }
}
