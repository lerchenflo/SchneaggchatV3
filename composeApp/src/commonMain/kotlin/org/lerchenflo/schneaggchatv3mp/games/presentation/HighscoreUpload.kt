package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_confirm
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_decline
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_done
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_failed
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_message
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_no_accounts
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_retry
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_skipped_players
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_title
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_uploading
import schneaggchatv3mp.composeapp.generated.resources.highscore_upload_wins_message

/** The confirmation dialog; shown while asking, uploading or after a failed attempt. */
@Composable
fun HighscoreUploadDialog(
    state: HighscoreUploadState,
    onUpload: () -> Unit,
    onDecline: () -> Unit,
) {
    val visible = state.status == HighscoreUploadStatus.ASKING ||
        state.status == HighscoreUploadStatus.UPLOADING ||
        state.status == HighscoreUploadStatus.FAILED
    if (!visible) return
    val uploading = state.status == HighscoreUploadStatus.UPLOADING

    AlertDialog(
        // A decision is required: tapping outside must not silently skip or upload
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.highscore_upload_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        if (state.winsOnly) Res.string.highscore_upload_wins_message
                        else Res.string.highscore_upload_message
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = row.playerName,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = row.scoreText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (state.skippedPlayerCount > 0) {
                    Text(
                        text = stringResource(Res.string.highscore_upload_skipped_players, state.skippedPlayerCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.status == HighscoreUploadStatus.FAILED) {
                    Text(
                        text = stringResource(Res.string.highscore_upload_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (uploading) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(stringResource(Res.string.highscore_upload_uploading))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpload, enabled = !uploading) {
                Text(
                    stringResource(
                        if (state.status == HighscoreUploadStatus.FAILED) Res.string.highscore_upload_retry
                        else Res.string.highscore_upload_confirm
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline, enabled = !uploading) {
                Text(stringResource(Res.string.highscore_upload_decline))
            }
        }
    )
}

/**
 * The end screen's last word on the leaderboard: either the results went up, or nobody in the
 * game had an account to put them on.
 */
@Composable
fun HighscoreUploadDoneText(state: HighscoreUploadState, modifier: Modifier = Modifier) {
    val uploaded = state.status == HighscoreUploadStatus.UPLOADED
    val text = when (state.status) {
        HighscoreUploadStatus.UPLOADED -> Res.string.highscore_upload_done
        HighscoreUploadStatus.NO_ACCOUNTS -> Res.string.highscore_upload_no_accounts
        else -> return
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = if (uploaded) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
