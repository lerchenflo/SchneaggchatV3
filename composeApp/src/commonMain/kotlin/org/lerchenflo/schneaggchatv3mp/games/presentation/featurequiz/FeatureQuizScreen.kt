package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizAnswerResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoresDialog
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizCard
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizOption
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizFeatureCard
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizHeader
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizModePicker
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizNextButton
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizOptionLook
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizOptionRow
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizProgress
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizRoundOver
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizSkipButton
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizVerdictCard
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizVoteCard
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureQuizWishCard
import org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements.FeatureVotingDone
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_empty
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finish
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_next
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_title
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_vote_prompt
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wish_prompt
import schneaggchatv3mp.composeapp.generated.resources.featurevoting_empty
import schneaggchatv3mp.composeapp.generated.resources.featurevoting_finish
import schneaggchatv3mp.composeapp.generated.resources.featurevoting_skip

/** The feedback board's vote buttons for one entry, handed in by the app so games don't depend on the feedback UI. */
typealias FeedbackVoteRowSlot = @Composable (entry: FeedbackEntry, onVoteClick: (FeedbackVote) -> Unit) -> Unit

@Composable
fun FeatureQuizScreenRoot(voteRow: FeedbackVoteRowSlot) {
    val viewModel = koinViewModel<FeatureQuizViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    FeatureQuizScreen(
        state = state,
        onAction = viewModel::onAction,
        voteRow = voteRow,
    )
}

@Composable
fun FeatureQuizScreen(
    state: FeatureQuizState,
    onAction: (FeatureQuizAction) -> Unit,
    voteRow: FeedbackVoteRowSlot,
) {
    var showHighscores by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.featurequiz_title),
            onBackClick = { onAction(FeatureQuizAction.OnBackClick) },
        )
        HorizontalDivider()

        // Outside the sliding cards, so the progress bar fills up smoothly from card to card
        if (state.isPlaying) {
            val headerModifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            // The vote-only mode has no time and no points
            if (state.mode == FeatureQuizMode.VOTE) {
                FeatureQuizProgress(
                    cardNumber = state.currentIndex + 1,
                    cardCount = state.cards.size,
                    modifier = headerModifier,
                )
            } else {
                FeatureQuizHeader(
                    cardNumber = state.currentIndex + 1,
                    cardCount = state.cards.size,
                    elapsedMillis = state.elapsedMillis,
                    points = state.points,
                    modifier = headerModifier,
                )
            }
        }

        // Each card slides in from the side; the outgoing card keeps the state it was shown with
        AnimatedContent(
            targetState = state,
            contentKey = { it.screenKey() },
            transitionSpec = {
                (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -width / 3 } + fadeOut())
            },
            label = "featureQuizCard",
            modifier = Modifier.weight(1f),
        ) { shown ->
            val card = shown.currentCard
            when {
                shown.isChoosingMode -> FeatureQuizModePicker(
                    onQuizClick = { onAction(FeatureQuizAction.OnModeClick(FeatureQuizMode.QUIZ)) },
                    onVoteClick = { onAction(FeatureQuizAction.OnModeClick(FeatureQuizMode.VOTE)) },
                )

                shown.isLoading -> CenteredBox { CircularProgressIndicator() }

                shown.cards.isEmpty() -> CenteredBox {
                    Text(
                        text = stringResource(
                            if (shown.mode == FeatureQuizMode.VOTE) Res.string.featurevoting_empty else Res.string.featurequiz_empty
                        ),
                        textAlign = TextAlign.Center,
                    )
                }

                (shown.isFinished || card == null) && shown.mode == FeatureQuizMode.VOTE -> FeatureVotingDone(
                    votedCount = shown.votedCount,
                    skippedCount = shown.skippedCount,
                    onDoneClick = { onAction(FeatureQuizAction.OnBackClick) },
                )

                shown.isFinished || card == null -> FeatureQuizRoundOver(
                    points = shown.points,
                    questionCount = shown.questionCount,
                    elapsedMillis = shown.elapsedMillis,
                    onPlayAgainClick = { onAction(FeatureQuizAction.OnPlayAgainClick) },
                    onHighscoresClick = { showHighscores = true },
                    onDoneClick = { onAction(FeatureQuizAction.OnBackClick) },
                )

                else -> CardContent(state = shown, card = card, onAction = onAction, voteRow = voteRow)
            }
        }
    }

    if (showHighscores) {
        HighscoresDialog(
            game = GameId.FEATURE_QUIZ,
            initialDifficulty = GameDifficulty.MEDIUM,
            onDismiss = { showHighscores = false },
        )
    }
}

/** Which page the quiz shows; a new key slides a new page in. */
private fun FeatureQuizState.screenKey(): String = when {
    isChoosingMode -> "mode"
    isLoading -> "loading"
    cards.isEmpty() -> "empty"
    isFinished -> "finished"
    else -> "card-$currentIndex"
}

@Composable
private fun CardContent(
    state: FeatureQuizState,
    card: FeatureQuizCard,
    onAction: (FeatureQuizAction) -> Unit,
    voteRow: FeedbackVoteRowSlot,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        when (card) {
            is FeatureQuizCard.Question -> QuestionContent(card = card, picked = state.pickedOption, onAction = onAction)
            is FeatureQuizCard.Wish -> FeatureQuizWishCard(entry = card.entry)
            is FeatureQuizCard.Feature -> FeatureQuizFeatureCard(entry = card.entry)
        }

        AnimatedVisibility(visible = state.showsVoteRow, enter = fadeIn() + expandVertically()) {
            FeatureQuizVoteCard(
                prompt = stringResource(
                    if (card is FeatureQuizCard.Wish) Res.string.featurequiz_wish_prompt else Res.string.featurequiz_vote_prompt
                ),
            ) {
                voteRow(card.entry.withVote(state.pendingVote)) { onAction(FeatureQuizAction.OnVoteClick(it)) }
            }
        }

        AnimatedVisibility(visible = !state.isAnswering, enter = fadeIn() + expandVertically()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.canSkip) {
                    FeatureQuizSkipButton(
                        text = stringResource(Res.string.featurevoting_skip),
                        onClick = { onAction(FeatureQuizAction.OnSkipClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
                FeatureQuizNextButton(
                    text = stringResource(state.nextButtonText()),
                    icon = if (state.isLastCard) Icons.Filled.Flag else Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = state.canContinue,
                    onClick = { onAction(FeatureQuizAction.OnNextClick) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The vote-only mode has no result to show, it just finishes. */
private fun FeatureQuizState.nextButtonText(): StringResource = when {
    !isLastCard -> Res.string.featurequiz_next
    mode == FeatureQuizMode.VOTE -> Res.string.featurevoting_finish
    else -> Res.string.featurequiz_finish
}

@Composable
private fun QuestionContent(
    card: FeatureQuizCard.Question,
    picked: FeatureQuizOption?,
    onAction: (FeatureQuizAction) -> Unit,
) {
    Text(
        text = stringResource(card.question),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        card.options.forEachIndexed { index, option ->
            FeatureQuizOptionRow(
                letter = 'A' + index,
                text = stringResource(option.text),
                look = option.lookFor(picked),
                onClick = { onAction(FeatureQuizAction.OnOptionClick(option)) },
            )
        }
    }

    AnimatedVisibility(visible = picked != null, enter = fadeIn() + expandVertically()) {
        if (picked != null) {
            FeatureQuizVerdictCard(
                result = picked.result,
                correctAnswer = stringResource(card.correctOption.text),
                explanation = stringResource(card.explanation),
            )
        }
    }
}

private fun FeatureQuizOption.lookFor(picked: FeatureQuizOption?): FeatureQuizOptionLook = when {
    picked == null -> FeatureQuizOptionLook.OPEN
    result == FeatureQuizAnswerResult.CORRECT -> FeatureQuizOptionLook.CORRECT
    this == picked -> FeatureQuizOptionLook.WRONG_PICK
    else -> FeatureQuizOptionLook.DIMMED
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        content()
    }
}
