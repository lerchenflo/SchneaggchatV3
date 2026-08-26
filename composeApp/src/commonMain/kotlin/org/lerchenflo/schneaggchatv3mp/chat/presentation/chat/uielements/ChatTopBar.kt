package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.UserButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.BackButton
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.chat_last_seen_status
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online

/**
 * Chat screen top bar: back button, profile picture, name and presence text ("Online" while
 * connected, otherwise the persisted last-seen time).
 */
@Composable
fun ChatTopBar(
    chatPartner: ChatListItem?,
    ownId: String,
    onBackClick: () -> Unit,
    onPartnerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp, 0.dp, 10.dp, 0.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BackButton(
            onBackClick = onBackClick
        )

        chatPartner?.let { partner ->
            val lastSeen = partner.lastSeen
            val presenceText = when {
                partner.isGroup -> ""
                partner.isOnline -> stringResource(Res.string.schneaggmap_user_online)
                lastSeen != null -> stringResource(Res.string.chat_last_seen_status, millisToTimeDateOrYesterday(lastSeen))
                else -> ""
            }

            UserButton(
                chat = partner,
                bottomTextOverride = presenceText,
                onClickGes = onPartnerClick,
                ownId = ownId,
            )
        }
    }
}
