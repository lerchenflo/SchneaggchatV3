package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.formatCountdown
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.rememberCountdownMillis
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_expired
import schneaggchatv3mp.composeapp.generated.resources.group_expires_at
import schneaggchatv3mp.composeapp.generated.resources.group_expires_in
import kotlin.time.Clock

/**
 * Small red box with the countdown of a group's delete timer and its date, shown to every member.
 * Only shows the timer; admins change it through the ⋮ of the chat details.
 */
@Composable
fun GroupExpiryBadge(
    expiresAt: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = groupExpiryText(expiresAt = expiresAt),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(
                        Res.string.group_expires_at,
                        millisToString(expiresAt, format = "dd.MM.yyyy HH:mm")
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun groupExpiryText(expiresAt: Long): String {
    val timeRemaining = rememberCountdownMillis(key = expiresAt) {
        expiresAt - Clock.System.now().toEpochMilliseconds()
    }

    return if (timeRemaining > 0) {
        stringResource(Res.string.group_expires_in, formatCountdown(timeRemaining))
    } else {
        stringResource(Res.string.group_expired)
    }
}
