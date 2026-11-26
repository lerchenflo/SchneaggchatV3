package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.lerchenflo.taptarget.tapTarget
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCard
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCardDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCardPart
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutEditButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.chat_details_about_title
import schneaggchatv3mp.composeapp.generated.resources.chat_details_friends_about_write_first
import schneaggchatv3mp.composeapp.generated.resources.about_friends_say
import schneaggchatv3mp.composeapp.generated.resources.chat_details_group_about_empty
import schneaggchatv3mp.composeapp.generated.resources.chat_details_group_about_title
import schneaggchatv3mp.composeapp.generated.resources.chat_details_group_about_write
import schneaggchatv3mp.composeapp.generated.resources.chat_details_status_says
import schneaggchatv3mp.composeapp.generated.resources.description_info_user
import schneaggchatv3mp.composeapp.generated.resources.status_info

/**
 * The texts about the chat in one [AboutCard]: for a friend their status ("emma sagt", only while
 * set, written by them, a tap explains that) and what all their friends write about them ("Freunde
 * sagen", editable by every friend but not by them); for a group its description. While nobody has
 * written about the friend, only the button to write the first text shows instead of that part, and
 * without a status there is no card at all.
 */
@Composable
fun ChatDetailsAboutCard(
    chat: ChatDetailsChat,
    isGroup: Boolean,
    onEditDescriptionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isGroup) {
        AboutCard(title = null, modifier = modifier) {
            AboutCardPart(
                label = stringResource(Res.string.chat_details_group_about_title),
                text = chat.description,
                emptyText = stringResource(Res.string.chat_details_group_about_empty),
                leading = { LabelIcon(Icons.AutoMirrored.Filled.Notes) },
                onEditClick = onEditDescriptionClick,
                emptyActionText = stringResource(Res.string.chat_details_group_about_write)
            )
        }
        return
    }

    val status = chat.status?.takeIf { it.isNotBlank() }
    val hasDescription = !chat.description.isNullOrBlank()
    val writeFirstButton = @Composable { buttonModifier: Modifier ->
        AboutEditButton(
            text = stringResource(Res.string.chat_details_friends_about_write_first),
            onClick = onEditDescriptionClick,
            modifier = buttonModifier.tapTarget("chat_details_description")
        )
    }

    if (status == null && !hasDescription) {
        writeFirstButton(modifier)
        return
    }

    val statusInfo = stringResource(Res.string.status_info)

    AboutCard(title = stringResource(Res.string.chat_details_about_title, chat.name), modifier = modifier) {
        if (status != null) {
            AboutCardPart(
                label = stringResource(Res.string.chat_details_status_says, chat.name),
                text = status,
                emptyText = "",
                isQuote = true,
                leading = {
                    ProfilePictureView(
                        filepath = chat.profilePictureUrl,
                        modifier = Modifier.size(22.dp).clip(CircleShape),
                        contentDescription = null
                    )
                },
                onClick = { SnackbarManager.showMessage(statusInfo) }
            )
        }

        if (hasDescription) {
            if (status != null) AboutCardDivider()
            AboutCardPart(
                label = stringResource(Res.string.about_friends_say),
                text = chat.description,
                emptyText = "",
                leading = { LabelIcon(Icons.Default.Groups) },
                hint = stringResource(Res.string.description_info_user, chat.name),
                onEditClick = onEditDescriptionClick,
                modifier = Modifier.tapTarget("chat_details_description")
            )
        } else {
            writeFirstButton(Modifier.padding(start = 16.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun LabelIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}
