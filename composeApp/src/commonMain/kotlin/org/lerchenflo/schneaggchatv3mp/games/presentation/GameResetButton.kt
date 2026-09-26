package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.game_reset_cancel
import schneaggchatv3mp.composeapp.generated.resources.game_reset_confirm
import schneaggchatv3mp.composeapp.generated.resources.game_reset_message
import schneaggchatv3mp.composeapp.generated.resources.game_reset_title

/**
 * Title bar button that throws away the running game (and its saved progress) after a confirmation,
 * for games that are restored automatically when reopened.
 */
@Composable
fun GameResetButton(onReset: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    IconButton(onClick = { confirming = true }) {
        Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = stringResource(Res.string.game_reset_title),
            tint = MaterialTheme.colorScheme.primary
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(Res.string.game_reset_title)) },
            text = { Text(stringResource(Res.string.game_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirming = false
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.game_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(Res.string.game_reset_cancel))
                }
            }
        )
    }
}
