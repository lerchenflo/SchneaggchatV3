package io.github.lerchenflo.voicemessages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lerchenflo.voicemessages.VoiceRecorder
import io.github.lerchenflo.voicemessages.internal.ensureDirectoryExists
import io.github.lerchenflo.voicemessages.internal.formatDurationMs
import io.github.lerchenflo.voicemessages.internal.randomFileName
import io.github.lerchenflo.voicemessages.internal.readVoiceMessageBytes
import io.github.lerchenflo.voicemessages.internal.rememberVoiceMessagesCacheDir
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Records a voice message, showing elapsed time while recording is active. Tap once to start,
 * tap again to stop; the finished recording's bytes are then delivered to [onRecorded] — what
 * you do with them (save, upload, hand to [VoiceMessagePlayer]) is entirely up to you.
 */
@Composable
fun VoiceMessageRecorder(
    modifier: Modifier = Modifier,
    onRecorded: (bytes: ByteArray) -> Unit,
) {
    val cacheDir = rememberVoiceMessagesCacheDir()
    val recorder = remember { VoiceRecorder() }
    val filePath = remember(cacheDir) { "$cacheDir/${randomFileName("recording")}.tmp" }

    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { recorder.stop() }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (true) {
                delay(1_000)
                elapsedSeconds++
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledIconButton(
            onClick = {
                if (isRecording) {
                    recorder.stop()
                    isRecording = false
                    onRecorded(readVoiceMessageBytes(filePath))
                } else {
                    scope.launch {
                        ensureDirectoryExists(cacheDir)
                        recorder.start(filePath)
                        isRecording = true
                    }
                }
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
            )
        }

        if (isRecording) {
            Text(
                text = formatDurationMs(elapsedSeconds * 1_000L),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
