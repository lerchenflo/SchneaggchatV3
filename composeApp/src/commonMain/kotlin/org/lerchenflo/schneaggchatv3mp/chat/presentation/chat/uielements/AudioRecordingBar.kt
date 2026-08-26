package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio.AudioPlayerView
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import org.lerchenflo.schneaggchatv3mp.utilities.formatMillis

/**
 * The id AudioPlayerView plays under while previewing an unsent recorded draft (not a real
 * message id yet - the message doesn't exist until sent).
 */
private const val DRAFT_AUDIO_MESSAGE_ID = "audio_record_tmp"

/**
 * Input-bar content while recording, or while a just-recorded voice message is staged to send:
 * live elapsed-time + waveform placeholder while recording, playback controls for the draft once
 * stopped.
 */
@Composable
fun AudioRecordingBar(
    content: SendMessageContent.AudioContent,
    maxVoiceMsgTime: Long,
    playbackProgress: StateFlow<PlaybackProgress>,
    onPlay: (messageId: String, path: String) -> Unit,
    onPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp) // Match standard TextField height
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (content.isRecording) {
            // Recording Dot (You could add an InfiniteTransition animation here for pulsing)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )

            Spacer(Modifier.width(8.dp))

            // Timer
            Text(
                text = formatMillis(content.duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Visualizer Placeholder
            // todo actual visualizer
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simple static bars to represent audio levels
                repeat(15) { index ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((10..24).random().dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
            }

            if (content.duration > maxVoiceMsgTime * 0.8) {
                Text(
                    text = formatMillis(maxVoiceMsgTime - content.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

        } else {
            val progress by playbackProgress.collectAsState()
            val isThisMessagePlaying = progress.messageId == DRAFT_AUDIO_MESSAGE_ID
            val currentPosition = if (isThisMessagePlaying) progress.currentPosition else 0L
            // The ViewModel measures the real recorded duration once when recording stops
            // (ChatViewModel.stopRecording), so content.duration is already accurate here - no
            // suspend call or LaunchedEffect needed to fetch it.
            val duration = if (isThisMessagePlaying) progress.duration else content.duration
            val isPlaying = isThisMessagePlaying && progress.isPlaying

            AudioPlayerView(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlay = { onPlay(DRAFT_AUDIO_MESSAGE_ID, content.audioPath) },
                onPause = onPause,
                onSeek = onSeek,
                modifier = Modifier.weight(1f)
            )
        }

        // Delete/Discard Button
        IconButton(
            onClick = onDiscard
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
