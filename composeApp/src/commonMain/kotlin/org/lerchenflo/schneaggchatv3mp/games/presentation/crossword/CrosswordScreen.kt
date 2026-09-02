package org.lerchenflo.schneaggchatv3mp.games.presentation.crossword

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordLanguage
import org.lerchenflo.schneaggchatv3mp.games.presentation.formatGameTime
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_check
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_language_english
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_language_german
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_language_question
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_load_failed
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_next_clue
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_previous_clue
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_restart
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_retry
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_solved
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_title

@Composable
fun CrosswordScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<CrosswordViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    // Leaving the game stops the timer so nothing keeps ticking in the background
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(CrosswordAction.StopGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_crossword_title),
            onBackClick = onBackClick
        )

        when {
            state.language == null -> LanguageSelection(
                onSelect = { viewmodel.onAction(CrosswordAction.SelectLanguage(it)) }
            )

            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.loadFailed -> LoadFailed(
                onRetry = { viewmodel.onAction(CrosswordAction.RetryLoad) },
                onPlayGerman = { viewmodel.onAction(CrosswordAction.SelectLanguage(CrosswordLanguage.GERMAN)) },
            )

            state.puzzle != null -> CrosswordContent(
                state = state,
                onAction = viewmodel::onAction,
            )
        }
    }
}

@Composable
private fun LanguageSelection(
    onSelect: (CrosswordLanguage) -> Unit,
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
            text = "🧩",
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.games_crossword_title),
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
                text = stringResource(Res.string.games_crossword_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.games_crossword_language_question),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSelect(CrosswordLanguage.ENGLISH) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.games_crossword_language_english))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSelect(CrosswordLanguage.GERMAN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.games_crossword_language_german))
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
            text = stringResource(Res.string.games_crossword_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text(text = stringResource(Res.string.games_crossword_retry))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The German puzzle is generated locally and always available offline
        OutlinedButton(onClick = onPlayGerman) {
            Text(text = stringResource(Res.string.games_crossword_language_german))
        }
    }
}

@Composable
private fun CrosswordContent(
    state: CrosswordState,
    onAction: (CrosswordAction) -> Unit,
) {
    val puzzle = state.puzzle ?: return
    val focusRequester = remember { FocusRequester() }

    // Grab the keyboard as soon as the puzzle is playable (and again after restart)
    LaunchedEffect(puzzle, state.isSolved) {
        if (!state.isSolved) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
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

            IconButton(onClick = { onAction(CrosswordAction.CheckPuzzle) }) {
                Icon(
                    imageVector = Icons.Default.Spellcheck,
                    contentDescription = stringResource(Res.string.games_crossword_check),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { onAction(CrosswordAction.RestartGame) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.games_crossword_restart),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        ClueBanner(state = state, onAction = onAction)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!state.isSolved) {
                CrosswordInputField(
                    focusRequester = focusRequester,
                    onAction = onAction,
                )
            }

            // Size the grid to fit the remaining space in BOTH dimensions so the
            // whole screen fits without scrolling on phones and desktop alike
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val attributionSpace = if (puzzle.sourceInfo != null) 22.dp else 0.dp
                val cellSize = minOf(
                    maxWidth / puzzle.cols,
                    (maxHeight - attributionSpace) / puzzle.rows
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CrosswordGrid(
                        state = state,
                        onCellTapped = {
                            onAction(CrosswordAction.CellTapped(it))
                            // Re-tapping the grid brings the soft keyboard back on mobile
                            if (!state.isSolved) focusRequester.requestFocus()
                        },
                        modifier = Modifier.size(
                            width = cellSize * puzzle.cols,
                            height = cellSize * puzzle.rows
                        )
                    )

                    puzzle.sourceInfo?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (state.isSolved) {
                SolvedCard(elapsedMillis = state.elapsedMillis)
            }
        }
    }
}

/**
 * Invisible 1dp text field that owns the focus while playing: it opens the
 * soft keyboard on mobile and receives hardware key events on desktop.
 * Letters arrive through onValueChange (works with any IME), while backspace
 * and enter are intercepted as key events so they also work when the field
 * text is empty.
 */
@Composable
private fun CrosswordInputField(
    focusRequester: FocusRequester,
    onAction: (CrosswordAction) -> Unit,
) {
    // A single sentinel space keeps the cursor at position 1 so deletions are observable
    var fieldValue by remember { mutableStateOf(TextFieldValue(" ", selection = TextRange(1))) }

    BasicTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            val typed = newValue.text.drop(1)
            typed.forEach { char ->
                if (char.isLetter()) onAction(CrosswordAction.KeyPressed(char.uppercaseChar()))
            }
            if (newValue.text.isEmpty()) onAction(CrosswordAction.Backspace)
            fieldValue = TextFieldValue(" ", selection = TextRange(1))
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onAction(CrosswordAction.NextClue) }
        ),
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onAction(CrosswordAction.NextClue)
                        true
                    }

                    Key.Backspace -> {
                        onAction(CrosswordAction.Backspace)
                        true
                    }

                    else -> false
                }
            }
    )
}

@Composable
private fun ClueBanner(
    state: CrosswordState,
    onAction: (CrosswordAction) -> Unit,
) {
    val clue = state.currentClue

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onAction(CrosswordAction.PreviousClue) }) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(Res.string.games_crossword_previous_clue)
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = clue?.let {
                    val arrow = if (it.direction == CrosswordDirection.ACROSS) "→" else "↓"
                    "${it.number} $arrow  ${it.text}"
                } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        IconButton(onClick = { onAction(CrosswordAction.NextClue) }) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(Res.string.games_crossword_next_clue)
            )
        }
    }
}

@Composable
private fun CrosswordGrid(
    state: CrosswordState,
    onCellTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val puzzle = state.puzzle ?: return
    val textMeasurer = rememberTextMeasurer()

    // Every background is paired with its matching "on" color so letters stay
    // readable in both light and dark theme
    val blockColor = MaterialTheme.colorScheme.inverseSurface
    val cellColor = MaterialTheme.colorScheme.surface
    val cellLetterColor = MaterialTheme.colorScheme.onSurface
    val cellNumberColor = MaterialTheme.colorScheme.onSurfaceVariant
    val wordHighlightColor = MaterialTheme.colorScheme.secondaryContainer
    val wordLetterColor = MaterialTheme.colorScheme.onSecondaryContainer
    val selectedColor = MaterialTheme.colorScheme.primary
    val selectedLetterColor = MaterialTheme.colorScheme.onPrimary
    val wrongColor = MaterialTheme.colorScheme.errorContainer
    val wrongLetterColor = MaterialTheme.colorScheme.onErrorContainer
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    val highlightedCells = state.currentClue?.cells?.toSet() ?: emptySet()

    Canvas(
        modifier = modifier
            .pointerInput(puzzle) {
                detectTapGestures { offset ->
                    val cellSize = size.width.toFloat() / puzzle.cols
                    val col = (offset.x / cellSize).toInt().coerceIn(0, puzzle.cols - 1)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, puzzle.rows - 1)
                    onCellTapped(row * puzzle.cols + col)
                }
            }
    ) {
        val cellSize = size.width / puzzle.cols
        val letterStyle = TextStyle(
            fontSize = (cellSize * 0.55f).toSp(),
            fontWeight = FontWeight.Bold,
        )
        val numberStyle = TextStyle(
            fontSize = (cellSize * 0.26f).toSp(),
        )

        for (index in puzzle.solution.indices) {
            val row = index / puzzle.cols
            val col = index % puzzle.cols
            val topLeft = Offset(col * cellSize, row * cellSize)
            val cellRect = Size(cellSize, cellSize)

            if (puzzle.solution[index] == null) {
                drawRect(color = blockColor, topLeft = topLeft, size = cellRect)
                continue
            }

            val (background, foreground) = when {
                index == state.selectedCell -> selectedColor to selectedLetterColor
                index in state.wrongCells -> wrongColor to wrongLetterColor
                index in highlightedCells -> wordHighlightColor to wordLetterColor
                else -> cellColor to cellLetterColor
            }
            drawRect(color = background, topLeft = topLeft, size = cellRect)

            val number = puzzle.cellNumbers[index]
            if (number != 0) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(number.toString()),
                    style = numberStyle.copy(
                        color = if (index == state.selectedCell) selectedLetterColor else cellNumberColor
                    ),
                    topLeft = topLeft + Offset(cellSize * 0.06f, 0f)
                )
            }

            state.entries[index]?.let { letter ->
                val layout = textMeasurer.measure(AnnotatedString(letter.toString()), letterStyle)
                drawText(
                    textLayoutResult = layout,
                    color = foreground,
                    topLeft = topLeft + Offset(
                        (cellSize - layout.size.width) / 2f,
                        (cellSize - layout.size.height) / 2f
                    )
                )
            }
        }

        // Grid lines on top
        for (c in 0..puzzle.cols) {
            drawLine(
                color = gridLineColor,
                start = Offset(c * cellSize, 0f),
                end = Offset(c * cellSize, puzzle.rows * cellSize),
                strokeWidth = 1f
            )
        }
        for (r in 0..puzzle.rows) {
            drawLine(
                color = gridLineColor,
                start = Offset(0f, r * cellSize),
                end = Offset(puzzle.cols * cellSize, r * cellSize),
                strokeWidth = 1f
            )
        }

        // Outline around the boxes of the active word so its span is obvious
        state.currentClue?.cells?.let { cells ->
            if (cells.isNotEmpty()) {
                val startRow = cells.first() / puzzle.cols
                val startCol = cells.first() % puzzle.cols
                val endRow = cells.last() / puzzle.cols
                val endCol = cells.last() % puzzle.cols
                val strokeWidth = (cellSize * 0.08f).coerceAtLeast(2f)
                drawRect(
                    color = selectedColor,
                    topLeft = Offset(startCol * cellSize, startRow * cellSize),
                    size = Size(
                        (endCol - startCol + 1) * cellSize,
                        (endRow - startRow + 1) * cellSize
                    ),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
private fun SolvedCard(
    elapsedMillis: Long,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.games_crossword_solved, formatGameTime(elapsedMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
