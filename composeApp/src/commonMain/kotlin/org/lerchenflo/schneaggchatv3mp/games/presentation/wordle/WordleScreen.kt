package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.WORDLE_MAX_GUESSES
import org.lerchenflo.schneaggchatv3mp.games.domain.WORDLE_WORD_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLetterState
import org.lerchenflo.schneaggchatv3mp.games.presentation.formatGameTime
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_delete
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_failed
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_guess_counter
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_new_word
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_language_english
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_language_german
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_language_question
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_load_failed
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_retry
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_solved
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_title
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_too_short
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_unknown_word
import kotlin.math.roundToInt

@Composable
fun WordleScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<WordleViewModel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()
    val restoreChecked by viewmodel.restoreChecked.collectAsStateWithLifecycle()

    // Leaving the screen persists the progress so it can be picked up again later
    DisposableEffect(Unit) {
        viewmodel.onAction(WordleAction.EnterGame)
        onDispose { viewmodel.onAction(WordleAction.LeaveGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_wordle_title),
            onBackClick = onBackClick
        )

        when {
            // Checked first so a restored game is not preceded by a flash of the language chooser
            !restoreChecked || state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.language == null -> LanguageSelection(
                onSelect = { viewmodel.onAction(WordleAction.SelectLanguage(it)) }
            )

            state.loadFailed -> LoadFailed(
                onRetry = { viewmodel.onAction(WordleAction.RetryLoad) },
                onPlayGerman = { viewmodel.onAction(WordleAction.SelectLanguage(WordleLanguage.GERMAN)) },
            )

            state.puzzle != null -> WordleContent(
                state = state,
                onAction = viewmodel::onAction,
            )
        }
    }
}

@Composable
private fun LanguageSelection(
    onSelect: (WordleLanguage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔤",
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.games_wordle_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(Res.string.games_wordle_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.games_wordle_language_question),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSelect(WordleLanguage.ENGLISH) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.games_wordle_language_english))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSelect(WordleLanguage.GERMAN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.games_wordle_language_german))
        }
    }
}

@Composable
private fun LoadFailed(
    onRetry: () -> Unit,
    onPlayGerman: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.games_wordle_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text(text = stringResource(Res.string.games_wordle_retry))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The German word comes from the local list and is always available offline
        OutlinedButton(onClick = onPlayGerman) {
            Text(text = stringResource(Res.string.games_wordle_language_german))
        }
    }
}

@Composable
private fun WordleContent(
    state: WordleState,
    onAction: (WordleAction) -> Unit,
) {
    val puzzle = state.puzzle ?: return
    val focusRequester = remember { FocusRequester() }
    // Only the submitted guesses can change the key colors
    val keyStates = remember(state.guesses) { state.keyStates() }

    // Hardware keyboards (desktop) type straight into the board
    LaunchedEffect(state.isFinished) {
        if (!state.isFinished) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        onAction(WordleAction.SubmitGuess)
                        true
                    }

                    event.key == Key.Backspace -> {
                        onAction(WordleAction.Backspace)
                        true
                    }

                    else -> {
                        val char = event.utf16CodePoint.toChar()
                        if (char.isLetter()) {
                            onAction(WordleAction.KeyPressed(char))
                            true
                        } else {
                            false
                        }
                    }
                }
            }
            .focusRequester(focusRequester)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatGameTime(state.elapsedMillis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(
                    Res.string.games_wordle_guess_counter,
                    state.guesses.size,
                    WORDLE_MAX_GUESSES
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = { onAction(WordleAction.NewWord) }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(Res.string.games_wordle_new_word),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Fixed slot so the board does not jump when a message appears
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            state.inputError?.let { error ->
                Text(
                    text = when (error) {
                        WordleInputError.TOO_SHORT -> stringResource(Res.string.games_wordle_too_short)
                        WordleInputError.UNKNOWN_WORD ->
                            stringResource(Res.string.games_wordle_unknown_word, state.rejectedWord)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            WordleBoard(state = state)
        }

        puzzle.sourceInfo?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // The result takes the keyboard's place so the finished board stays visible
        if (state.isFinished) {
            ResultCard(
                state = state,
                solution = puzzle.solution,
            )
        } else {
            WordleKeyboard(
                language = puzzle.language,
                keyStates = keyStates,
                onAction = onAction,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** How far the active row travels sideways when a guess is rejected. */
private const val SHAKE_DISTANCE = 12f

@Composable
private fun WordleBoard(
    state: WordleState,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.errorNonce) {
        if (state.errorNonce == 0) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 300
                0f at 0
                -SHAKE_DISTANCE at 50
                SHAKE_DISTANCE at 100
                -SHAKE_DISTANCE at 150
                SHAKE_DISTANCE at 200
                0f at 300
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Square tiles that fit both the remaining height and the width; the lower
        // bound keeps the size valid if the board is squeezed into almost no space
        val spacing = 4.dp
        val tileSize = minOf(
            (maxWidth - spacing * (WORDLE_WORD_LENGTH - 1)) / WORDLE_WORD_LENGTH,
            (maxHeight - spacing * (WORDLE_MAX_GUESSES - 1)) / WORDLE_MAX_GUESSES,
        ).coerceIn(16.dp, 64.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(WORDLE_MAX_GUESSES) { rowIndex ->
                val guess = state.guesses.getOrNull(rowIndex)
                val isActiveRow = guess == null && rowIndex == state.guesses.size

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = if (isActiveRow) {
                        Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }
                    } else {
                        Modifier
                    }
                ) {
                    repeat(WORDLE_WORD_LENGTH) { column ->
                        val letter = when {
                            guess != null -> guess.word.getOrNull(column)
                            isActiveRow -> state.currentInput.getOrNull(column)
                            else -> null
                        }
                        WordleTile(
                            letter = letter,
                            letterState = guess?.states?.getOrNull(column),
                            size = tileSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordleTile(
    letter: Char?,
    letterState: WordleLetterState?,
    size: Dp,
) {
    val target = letterState.tileColors()
    val background by animateColorAsState(
        targetValue = target.background,
        animationSpec = tween(durationMillis = 250)
    )
    val content by animateColorAsState(
        targetValue = target.content,
        animationSpec = tween(durationMillis = 250)
    )
    // An unrevealed tile is only outlined, a revealed one is filled
    val borderColor = if (letterState == null) {
        if (letter == null) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        letter?.let {
            Text(
                text = it.toString(),
                color = content,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.5f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


/** QWERTZ for German, QWERTY for English — only rows 1 and 3 differ. */
private fun keyboardRows(language: WordleLanguage): List<String> {
    val topRow = if (language == WordleLanguage.GERMAN) "QWERTZUIOP" else "QWERTYUIOP"
    val bottomRow = if (language == WordleLanguage.GERMAN) "YXCVBNM" else "ZXCVBNM"
    return listOf(topRow, "ASDFGHJKL", bottomRow)
}

@Composable
private fun WordleKeyboard(
    language: WordleLanguage,
    keyStates: Map<Char, WordleLetterState>,
    onAction: (WordleAction) -> Unit,
) {
    val rows = remember(language) { keyboardRows(language) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // A full row checks itself, so the last row only needs backspace —
                // the empty slot opposite keeps its letters aligned with the rows above
                if (rowIndex == rows.lastIndex) {
                    Spacer(modifier = Modifier.weight(1.8f))
                }

                row.forEach { letter ->
                    WordleLetterKey(
                        letter = letter,
                        letterState = keyStates[letter] ?: WordleLetterState.UNUSED,
                        onClick = { onAction(WordleAction.KeyPressed(letter)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (rowIndex == rows.lastIndex) {
                    WordleBackspaceKey(
                        onClick = { onAction(WordleAction.Backspace) },
                        modifier = Modifier.weight(1.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WordleLetterKey(
    letter: Char,
    letterState: WordleLetterState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = letterState.tileColors()
    val background by animateColorAsState(
        targetValue = target.background,
        animationSpec = tween(durationMillis = 250)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = background,
        contentColor = target.content,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WordleBackspaceKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = stringResource(Res.string.games_wordle_delete),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ResultCard(
    state: WordleState,
    solution: String,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (state.isSolved) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.isSolved) "🎉" else "😵",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (state.isSolved) {
                    stringResource(
                        Res.string.games_wordle_solved,
                        state.guesses.size,
                        WORDLE_MAX_GUESSES,
                        formatGameTime(state.elapsedMillis)
                    )
                } else {
                    stringResource(Res.string.games_wordle_failed, solution)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isSolved) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
