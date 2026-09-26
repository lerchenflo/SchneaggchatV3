package org.lerchenflo.schneaggchatv3mp.sharedUi.popups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.MAX_REACTION_LENGTH
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.enter_reaction

/**
 * Free-text reaction input, capped at [MAX_REACTION_LENGTH] - the same bound the server enforces
 * on a reaction, so whatever is entered here can always be sent. Used both for a one-off custom
 * reaction on a message and for adding a quick reaction in the appearance settings.
 */
@Composable
fun ReactionInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= MAX_REACTION_LENGTH) input = it },
                    label = { Text(stringResource(Res.string.enter_reaction)) },
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${input.length} / $MAX_REACTION_LENGTH",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    isError = input.length == MAX_REACTION_LENGTH
                )
            }
        },
        confirmButton = {
            NormalButton(
                text = stringResource(Res.string.add),
                onClick = {
                    if (input.isNotBlank()) {
                        onConfirm(input.trim())
                    }
                },
                primary = true
            )
        },
        dismissButton = {
            NormalButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss,
                primary = false
            )
        }
    )
}
