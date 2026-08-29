package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.poll

/**
 * Dropdown for attaching media to a message, opened from the input bar's "add" button.
 */
@Composable
fun AddMediaMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onPollClick: () -> Unit,
) {
    if (expanded) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            AddMediaOptions.entries.forEach { option ->

                /*
                val dev = SessionCache.requireLoggedIn()?.developer ?: return@DropdownMenu
                if(!dev) {
                    if (option == AddMediaOptions.AUDIO) return@forEach //Removes audio if no dev
                }

                 */


                DropdownMenuItem(
                    text = {
                        Row {
                            Icon(
                                imageVector = option.getIcon(),
                                contentDescription = option.toUiText().asString(),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = option.toUiText().asString()
                            )
                        }
                    },
                    onClick = {
                        onDismiss()
                        option.getAction(
                            onPollAction = onPollClick,
                            onImageAction = onImageClick, // todo actions übergeaba
                        )
                    }
                )
            }
        }
    }
}

enum class AddMediaOptions{
    IMAGE,
    POLL;

    fun toUiText(): UiText = when (this) {
        IMAGE -> UiText.StringResourceText(Res.string.image)
        POLL -> UiText.StringResourceText(Res.string.poll)
        //AUDIO   -> UiText.StringResourceText(Res.string.audio)
    }
    fun getIcon(): ImageVector = when (this) {
        IMAGE -> Icons.Default.Image
        POLL -> Icons.Default.Poll
        //AUDIO   -> Icons.Default.Headphones
    }

    fun getAction(
        onImageAction: () -> Unit,
        onPollAction: () -> Unit,
        //onAudioAction: () -> Unit,
    ): Unit = when (this) {
        IMAGE -> onImageAction()
        POLL -> onPollAction()
        //AUDIO -> onAudioAction()
    }
}
