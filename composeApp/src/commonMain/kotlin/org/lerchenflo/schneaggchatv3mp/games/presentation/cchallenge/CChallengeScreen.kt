package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoresDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.formatGameTime
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.rememberToday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.game_exit
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_bug_keyword_typo
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_bug_missing_parenthesis
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_bug_missing_quote
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_bug_missing_semicolon
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_bug_wrong_bracket
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_check
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_come_back
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_failed
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_number
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_order_feedback
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_order_slot
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_output
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_prompt_fill_blank
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_prompt_fix_syntax
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_prompt_order_lines
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_prompt_predict_output
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_result_points
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_solution
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_solved
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_streak
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_streak_badge
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_title
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_tries_left
import schneaggchatv3mp.composeapp.generated.resources.games_cchallenge_wrong
import schneaggchatv3mp.composeapp.generated.resources.highscores_title

@Composable
fun CChallengeScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<CChallengeViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()
    val restoreChecked by viewmodel.restoreChecked.collectAsStateWithLifecycle()

    // Leaving the screen persists the progress so it can be picked up again later
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(CChallengeAction.Leave) }
    }

    // The daily challenge rolls over at local midnight, also while this screen stays open
    val today = rememberToday()
    LaunchedEffect(today) {
        viewmodel.onAction(CChallengeAction.CheckDayChanged)
    }

    CChallengeScreen(
        state = state,
        restoreChecked = restoreChecked,
        onAction = viewmodel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
private fun CChallengeScreen(
    state: CChallengeState,
    restoreChecked: Boolean,
    onAction: (CChallengeAction) -> Unit,
    onBackClick: () -> Unit,
) {
    var showHighscores by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.games_cchallenge_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChallengeHeader(state)

                if (state.isFinished) {
                    ResultCard(
                        state = state,
                        onShowHighscores = { showHighscores = true },
                        onExit = onBackClick,
                    )
                    Solution(state.challenge)
                } else {
                    Puzzle(state = state, onAction = onAction)
                }
            }

            if (restoreChecked && !state.isStarted && !state.isFinished) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_cchallenge_title),
                    explanation = stringResource(Res.string.games_cchallenge_instructions),
                    onStart = { onAction(CChallengeAction.Start) }
                )
            }
        }
    }

    if (showHighscores) {
        HighscoresDialog(
            game = GameId.C_CHALLENGE,
            initialDifficulty = GameDifficulty.MEDIUM,
            onDismiss = { showHighscores = false }
        )
    }
}

@Composable
private fun ChallengeHeader(state: CChallengeState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.games_cchallenge_number, state.challenge.number),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (!state.isFinished) {
            Text(
                text = formatGameTime(state.elapsedMillis),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = stringResource(Res.string.games_cchallenge_streak_badge, state.streak.current),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Puzzle(
    state: CChallengeState,
    onAction: (CChallengeAction) -> Unit,
) {
    val challenge = state.challenge
    val colors = MaterialTheme.colorScheme

    Text(
        text = stringResource(challenge.type.prompt()),
        style = MaterialTheme.typography.bodyLarge,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.games_cchallenge_tries_left, state.triesLeft),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (state.triesUsed > 0) {
            Text(
                text = stringResource(Res.string.games_cchallenge_wrong),
                style = MaterialTheme.typography.labelLarge,
                color = colors.error,
            )
        }
    }

    when (challenge.type) {
        CChallengeType.FIX_SYNTAX -> {
            CCodeBlock(
                rows = challenge.codeLines.mapIndexed { index, line ->
                    CCodeRow(
                        text = line,
                        number = index + 1,
                        background = if (index in state.wrongPicks) colors.errorContainer else null,
                        onClick = if (line.isBlank()) null else ({ onAction(CChallengeAction.SelectLine(index)) }),
                    )
                }
            )
        }

        CChallengeType.FILL_BLANK, CChallengeType.PREDICT_OUTPUT -> {
            CCodeBlock(
                rows = challenge.codeLines.mapIndexed { index, line ->
                    CCodeRow(
                        text = line,
                        number = index + 1,
                        background = if (challenge.type == CChallengeType.FILL_BLANK && index == challenge.highlightLine) {
                            colors.primaryContainer
                        } else null,
                    )
                }
            )
            AnswerOptions(state = state, onAction = onAction)
        }

        CChallengeType.ORDER_LINES -> OrderLinesPuzzle(state = state, onAction = onAction)
    }
}

@Composable
private fun AnswerOptions(
    state: CChallengeState,
    onAction: (CChallengeAction) -> Unit,
) {
    state.challenge.options.forEachIndexed { index, option ->
        val wrong = index in state.wrongPicks
        OutlinedButton(
            onClick = { onAction(CChallengeAction.SelectOption(index)) },
            enabled = !wrong,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = option,
                style = CodeTextStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrderLinesPuzzle(
    state: CChallengeState,
    onAction: (CChallengeAction) -> Unit,
) {
    val challenge = state.challenge
    val colors = MaterialTheme.colorScheme
    val slotHint = stringResource(Res.string.games_cchallenge_order_slot)

    // Fixed code with the block being built spliced in; placed lines can be tapped to take them back
    val placed = state.orderPicked.mapIndexed { position, poolIndex ->
        CCodeRow(
            text = challenge.orderPool[poolIndex],
            background = colors.primaryContainer,
            onClick = { onAction(CChallengeAction.RemoveOrderLine(position)) },
        )
    }
    val emptySlots = List(challenge.orderPool.size - state.orderPicked.size) {
        CCodeRow(text = "    // $slotHint", background = colors.surfaceContainerHighest)
    }
    val rows = challenge.codeLines.take(challenge.highlightLine).map { CCodeRow(it) } +
        placed + emptySlots +
        challenge.codeLines.drop(challenge.highlightLine).map { CCodeRow(it) }
    CCodeBlock(rows = rows)

    // Remaining lines, without indentation so it does not give the nesting away
    challenge.orderPool.forEachIndexed { poolIndex, line ->
        if (poolIndex !in state.orderPicked) {
            OutlinedButton(
                onClick = { onAction(CChallengeAction.PickOrderLine(poolIndex)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = line.trim(),
                    style = CodeTextStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    state.orderCorrectCount?.let { correct ->
        Text(
            text = stringResource(Res.string.games_cchallenge_order_feedback, correct, challenge.orderPool.size),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.error,
        )
    }

    Button(
        onClick = { onAction(CChallengeAction.SubmitOrder) },
        enabled = state.orderPicked.size == challenge.orderPool.size,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.games_cchallenge_check))
    }
}

@Composable
private fun ResultCard(
    state: CChallengeState,
    onShowHighscores: () -> Unit,
    onExit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.solved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(if (state.solved) Res.string.games_cchallenge_solved else Res.string.games_cchallenge_failed),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.games_cchallenge_result_points, state.score, formatGameTime(state.elapsedMillis)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.games_cchallenge_streak, state.streak.current, state.streak.best),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.games_cchallenge_come_back),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onShowHighscores,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.highscores_title))
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.game_exit))
            }
        }
    }
}

@Composable
private fun Solution(challenge: CChallenge) {
    Text(
        text = stringResource(Res.string.games_cchallenge_solution),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )

    challenge.bug?.let { bug ->
        Text(
            text = stringResource(bug.explanation()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (challenge.type == CChallengeType.PREDICT_OUTPUT) {
        Text(
            text = stringResource(Res.string.games_cchallenge_output, challenge.options[challenge.correctOption]),
            style = CodeTextStyle,
        )
    }

    CCodeBlock(
        rows = challenge.solutionLines.mapIndexed { index, line ->
            CCodeRow(
                text = line,
                number = index + 1,
                background = if (index in challenge.solutionHighlight) MaterialTheme.colorScheme.primaryContainer else null,
            )
        }
    )
}

private fun CChallengeType.prompt(): StringResource = when (this) {
    CChallengeType.FIX_SYNTAX -> Res.string.games_cchallenge_prompt_fix_syntax
    CChallengeType.FILL_BLANK -> Res.string.games_cchallenge_prompt_fill_blank
    CChallengeType.PREDICT_OUTPUT -> Res.string.games_cchallenge_prompt_predict_output
    CChallengeType.ORDER_LINES -> Res.string.games_cchallenge_prompt_order_lines
}

private fun CSyntaxBug.explanation(): StringResource = when (this) {
    CSyntaxBug.MISSING_SEMICOLON -> Res.string.games_cchallenge_bug_missing_semicolon
    CSyntaxBug.MISSING_PARENTHESIS -> Res.string.games_cchallenge_bug_missing_parenthesis
    CSyntaxBug.MISSING_QUOTE -> Res.string.games_cchallenge_bug_missing_quote
    CSyntaxBug.KEYWORD_TYPO -> Res.string.games_cchallenge_bug_keyword_typo
    CSyntaxBug.WRONG_BRACKET -> Res.string.games_cchallenge_bug_wrong_bracket
}
