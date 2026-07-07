package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_morse_clear
import schneaggchatv3mp.composeapp.generated.resources.games_morse_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_morse_title
import schneaggchatv3mp.composeapp.generated.resources.morse_challenge_start
import schneaggchatv3mp.composeapp.generated.resources.morse_challenge_type_text
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val DASH_THRESHOLD_MS = 200

@Composable
fun MorseScreen(
    onBackClick: () -> Unit,
    viewModel: MorseViewModel
) {
    val state by viewModel.state.collectAsState()
    var explanationDismissed by rememberSaveable { mutableStateOf(false) }

    // Leaving the game ends the challenge so no timer keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewModel.exitChallenge() }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                ActivityTitle(
                    title = stringResource(Res.string.games_morse_title),
                    onBackClick = onBackClick
                )
            }

            IconButton(
                onClick = { viewModel.clear() },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.games_morse_clear))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val challenge = state.challenge

                if (challenge == null) {
                    Button(
                        onClick = viewModel::startChallenge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.morse_challenge_start))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    GameHud(
                        score = challenge.score.toLong(),
                        timeMillis = challenge.elapsedMillis,
                        onStop = viewModel::exitChallenge,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChallengeHeader(challenge = challenge)

                    Spacer(modifier = Modifier.height(8.dp))
                }

                CodeDisplay(state = state)

                Spacer(modifier = Modifier.height(8.dp))

                if (challenge == null) {
                    HistoryRow(history = state.history)

                    Spacer(modifier = Modifier.height(8.dp))
                }

                MorseTreeView(
                    currentCode = state.currentCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PressButton(
                    onDot = viewModel::addDot,
                    onDash = viewModel::addDash
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.challenge?.isGameOver == true) {
                GameOverOverlay(
                    game = GameId.MORSE,
                    finalScore = state.challenge?.score?.toLong() ?: 0L,
                    onRestart = viewModel::startChallenge,
                    onExit = {
                        viewModel.exitChallenge()
                        onBackClick()
                    }
                )
            }

            if (!explanationDismissed && state.challenge == null) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_morse_title),
                    explanation = stringResource(Res.string.games_morse_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.startChallenge()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChallengeHeader(
    challenge: MorseChallengeState
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.morse_challenge_type_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "♥".repeat((CHALLENGE_MAX_ERRORS - challenge.errors).coerceAtLeast(0)),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    challenge.targetText.forEachIndexed { index, char ->
                        when {
                            index < challenge.currentIndex -> withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) { append(char) }

                            index == challenge.currentIndex -> withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) { append(char) }

                            else -> withStyle(
                                SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) { append(char) }
                        }
                    }
                },
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun CodeDisplay(state: MorseState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayCode = state.currentCode
            .replace(".", "·")
            .replace("-", "−")

        Text(
            text = if (state.invalid) "?" else displayCode.ifEmpty { "—" },
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                state.invalid -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = state.currentChar?.toString() ?: "",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryRow(history: List<Char>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = history.joinToString(" "),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun PressButton(onDot: () -> Unit, onDash: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        val mark = TimeSource.Monotonic.markNow()
                        val released = tryAwaitRelease()
                        if (released) {
                            val elapsed = mark.elapsedNow()
                            if (elapsed < DASH_THRESHOLD_MS.milliseconds) {
                                onDot()
                            } else {
                                onDash()
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "·  /  −",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 4.sp
            )
        }
    }
}
