package io.github.lerchenflo.voicemessages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lerchenflo.voicemessages.VoicePlayer
import io.github.lerchenflo.voicemessages.internal.deleteVoiceMessageFile
import io.github.lerchenflo.voicemessages.internal.ensureParentDirectoryExists
import io.github.lerchenflo.voicemessages.internal.formatDurationMs
import io.github.lerchenflo.voicemessages.internal.probeDurationMs
import io.github.lerchenflo.voicemessages.internal.randomFileName
import io.github.lerchenflo.voicemessages.internal.rememberVoiceMessagesCacheDir
import io.github.lerchenflo.voicemessages.internal.writeVoiceMessageBytes
import kotlinx.coroutines.delay

/**
 * Plays back the voice message at [filePath]: a play/pause button, a progress bar, and the
 * current playback position alongside the full length (e.g. "0:05 / 1:23").
 *
 * Only the file's duration metadata is read up front; the audio itself isn't loaded or
 * decoded until the user taps play.
 */
@Composable
fun VoiceMessagePlayer(
    filePath: String,
    modifier: Modifier = Modifier,
) {
    val player = remember(filePath) { VoicePlayer() }
    var isPlaying by remember(filePath) { mutableStateOf(false) }
    var positionMs by remember(filePath) { mutableStateOf(0L) }
    var durationMs by remember(filePath) { mutableStateOf(0L) }

    LaunchedEffect(filePath) {
        durationMs = probeDurationMs(filePath)
    }

    DisposableEffect(filePath) {
        onDispose { player.stop() }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.positionMs
            if (!player.isPlaying) {
                isPlaying = false
            }
            delay(200)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledIconButton(
            onClick = {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.play(filePath)
                    isPlaying = true
                }
            },
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
            )
        }

        val progress = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "${formatDurationMs(positionMs)} / ${formatDurationMs(durationMs)}",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Plays back the voice message held in [bytes]. The bytes are written to [filePath] once,
 * then played back through the [filePath]-based overload above — which reads only that
 * file's duration metadata up front and defers loading the audio itself until play is tapped.
 *
 * @param filePath where to write [bytes] before playing them back. Defaults to a randomly
 * named file in the library's scratch cache directory, which is deleted once this composable
 * leaves composition. Pass your own path (e.g. a file you already manage) to control exactly
 * where it's written and to keep the file around afterwards — otherwise every call writes to
 * a fresh, randomly named file with no way to know or reuse its location.
 */
@Composable
fun VoiceMessagePlayer(
    bytes: ByteArray,
    filePath: String? = null,
    modifier: Modifier = Modifier,
) {
    val cacheDir = rememberVoiceMessagesCacheDir()
    val resolvedFilePath = remember(cacheDir, filePath, bytes) {
        filePath ?: "$cacheDir/${randomFileName("playback")}.tmp"
    }
    var isReady by remember(resolvedFilePath) { mutableStateOf(false) }

    if (filePath == null) {
        DisposableEffect(resolvedFilePath) {
            onDispose { deleteVoiceMessageFile(resolvedFilePath) }
        }
    }

    LaunchedEffect(resolvedFilePath, bytes) {
        ensureParentDirectoryExists(resolvedFilePath)
        writeVoiceMessageBytes(resolvedFilePath, bytes)
        isReady = true
    }

    if (isReady) {
        VoiceMessagePlayer(filePath = resolvedFilePath, modifier = modifier)
    }
}
