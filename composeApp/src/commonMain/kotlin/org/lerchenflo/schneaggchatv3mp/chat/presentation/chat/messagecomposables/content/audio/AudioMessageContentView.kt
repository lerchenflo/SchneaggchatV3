package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress

@Composable
fun AudioMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false,
    onAction: (MessageAction) -> Unit = {},
    playbackProgress: StateFlow<PlaybackProgress>? = null,
) {

    val progress by playbackProgress?.collectAsState() ?: remember { mutableStateOf(PlaybackProgress()) }

    val isThisMessagePlaying = progress.messageId == message.id
    val currentPosition = if (isThisMessagePlaying) progress.currentPosition else 0L
    val duration = if (isThisMessagePlaying) progress.duration else 0L
    val isPlaying = isThisMessagePlaying && progress.isPlaying

    val textColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    AudioPlayerView(
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        onPlay = {
            onAction(
                MessageAction.PlayAudio(
                    messageId = message.id ?: "",
                    audioPath = message.audioPath ?: ""
                )
            )
        },
        onPause = {
            onAction(MessageAction.PauseAudio())
        },
        onSeek = {
            onAction(MessageAction.SeekAudio(it))
        },
        textColor = textColor,
        modifier = Modifier
            .fillMaxWidth(),

    )

}

@Composable
fun AudioPlayerView(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (position: Long) -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    onPause()
                } else {
                    onPlay()
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "pause" else "play",
                tint = textColor
            )
        }

        Spacer(Modifier.width(4.dp))

        // new variable so slider can be adjusted manually (seeking)
        var sliderPosition by remember { mutableStateOf(currentPosition.toFloat()) }
        var isUserDragging by remember { mutableStateOf(false) }
        LaunchedEffect(currentPosition) {
            if (!isUserDragging) {
                sliderPosition = currentPosition.toFloat()
            }
        }

        Slider(
            modifier = Modifier.weight(1f),
            value = sliderPosition,
            valueRange = 0f..maxOf(1f, duration.toFloat()),
            onValueChange = {
                onPause()
                isUserDragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                isUserDragging = false
                onSeek(sliderPosition.toLong())
            },
            colors = SliderDefaults.colors( // todo bessere farba usdenka. i bin noch ned ganz glücklich
                thumbColor = textColor,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${sliderPosition.toLong().toTimeString()}/\n${duration.toTimeString()}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

private fun Long.toTimeString(): String {
    val totalSeconds = this / 1000
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    val minutes = ((totalSeconds / 60) % 60).toString()
    return "$minutes:$seconds"
}