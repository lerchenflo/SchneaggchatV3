package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.lerchenflo.voicemessages.VoiceMessageFiles
import io.github.lerchenflo.voicemessages.VoicePlaybackState
import io.github.lerchenflo.voicemessages.ui.VoiceMessagePlayer
import io.github.lerchenflo.voicemessages.ui.VoiceMessagePlayerTexts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MediaPlaceholder
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.media_expired_voice
import schneaggchatv3mp.composeapp.generated.resources.voice_message_pause
import schneaggchatv3mp.composeapp.generated.resources.voice_message_play
import schneaggchatv3mp.composeapp.generated.resources.voice_message_position

/**
 * A voice message bubble: the library's player row for the stored file. [playbackProgress] is
 * null where nothing plays (previews, reply and option popups); the row then shows the length only.
 */
@Composable
fun AudioMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false,
    onAction: (MessageAction) -> Unit = {},
    playbackProgress: StateFlow<VoicePlaybackState>? = null,
    voiceFiles: VoiceMessageFiles = koinInject()
) {
    val messageId = message.id ?: ""
    // Messages store the bare file name; empty until the file is downloaded
    val fileName = message.audioPath.orEmpty()
    val filePath = remember(fileName) { if (fileName.isBlank()) "" else voiceFiles.path(fileName) }
    val playback = playbackProgress ?: remember { MutableStateFlow(VoicePlaybackState()) }
    val color = if (myMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    // Deleted on the server and not on this device: it will never come, so no player
    val hasFile = remember(fileName) { fileName.isNotBlank() && voiceFiles.exists(fileName) }
    if (message.mediaExpired && !hasFile) {
        MediaPlaceholder(
            icon = Icons.Outlined.MicOff,
            text = stringResource(Res.string.media_expired_voice),
            color = color,
            modifier = modifier,
        )
        return
    }

    VoiceMessagePlayer(
        id = messageId,
        filePath = filePath,
        playback = playback,
        texts = voiceMessagePlayerTexts(),
        onPlay = { onAction(MessageAction.PlayAudio(messageId = messageId, audioPath = fileName)) },
        onPause = { onAction(MessageAction.PauseAudio()) },
        onSeek = { position -> onAction(MessageAction.SeekAudio(messageId = messageId, position = position)) },
        color = color,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Screen reader texts of the voice message player, shared by the bubble and the draft in the input bar. */
@Composable
fun voiceMessagePlayerTexts(): VoiceMessagePlayerTexts {
    val play = stringResource(Res.string.voice_message_play)
    val pause = stringResource(Res.string.voice_message_pause)
    val position = stringResource(Res.string.voice_message_position)
    return remember(play, pause, position) { VoiceMessagePlayerTexts(play = play, pause = pause, position = position) }
}
