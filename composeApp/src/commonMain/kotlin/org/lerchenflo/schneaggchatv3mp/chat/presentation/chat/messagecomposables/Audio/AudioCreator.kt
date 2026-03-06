package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.Audio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.hyochan.audio.AudioSource
import kotlinx.io.IOException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugAudioDialog( // chatbot mit mehr chatbot aaa
    onDismiss: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onSend: () -> Unit = {},
) {

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<AudioSource.File?>(null) }


    // Ensure resources are released when the dialog is removed from composition.
    DisposableEffect(Unit) {
        onDispose {
            // Clean up any active recording/playback
            if (isRecording) {
                // TODO
            }
            if (isPlaying) {
                // TODO
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Audio Debug",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Recording controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onStartRecording()
                            isRecording = true
                        },
                        enabled = !isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Record")
                    }
                    Button(
                        onClick = {
                            onStopRecording()
                            isRecording = false
                        },
                        enabled = isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop Record")
                    }
                }

                // Playback controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onPlay()
                            isPlaying = true
                        },
                        enabled = audioFile != null && !isPlaying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play")
                    }
                    Button(
                        onClick = {
                            onPause()
                            isPlaying = false
                        },
                        enabled = isPlaying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pause")
                    }
                }

                // Optional: show current state
                Text(
                    text = when {
                        isRecording -> "🔴 Recording..."
                        isPlaying -> "▶️ Playing..."
                        audioFile != null -> "✅ Recording saved"
                        else -> "No recording"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    onClick = onSend,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Send")
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}