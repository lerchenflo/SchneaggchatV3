package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.edit

/**
 * Trailing button(s) of the chat input row: send / mic / stop-recording while composing, or
 * cancel / confirm while editing an existing message.
 */
@Composable
fun SendActionButtons(
    isEditing: Boolean,
    isRecording: Boolean,
    hasContent: Boolean,
    isDesktop: Boolean,
    onSend: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    if (!isEditing) { // schoua ob mir gad a nachricht bearbeitend

        // show send when content is not empty or on desktop (no microphone implementation for desktop)
        if (hasContent || isDesktop) {
            if (isRecording) {
                IconButton(
                    onClick = onStopRecording
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        //tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // send button
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                    )
                }
            }
        } else {
            IconButton(
                onClick = onStartRecording,
                modifier = Modifier
                    .padding(5.dp)

            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                )
            }
        }

    } else {

        //Cancel reply button
        IconButton(
            onClick = onCancelEdit,
            modifier = Modifier
                .padding(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = stringResource(Res.string.cancel),
            )
        }

        //edit message button
        IconButton(
            onClick = onConfirmEdit,
            modifier = Modifier
                .padding(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(Res.string.edit),
            )
        }
        // mdtodo
    }
}
