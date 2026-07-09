package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapBetaTesterPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapGamesPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapGroupsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapIntroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapLeaderboardPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMapPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMessagesReceivedPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMessagesSentPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapOutroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapPasswordResetPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapReactionsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapRhythmPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapSocialPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapTopContactsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapTypingPage
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_load_failed
import schneaggchatv3mp.composeapp.generated.resources.recap_loading
import schneaggchatv3mp.composeapp.generated.resources.recap_retry
import kotlin.math.absoluteValue

private const val PAGE_AUTO_ADVANCE_MILLIS = 12000

@Composable
fun RecapScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<RecapViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    RecapScreen(
        state = state,
        onAction = viewModel::onAction,
        onClose = onBackClick
    )
}

@Composable
fun RecapScreen(
    state: RecapState,
    onAction: (RecapAction) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        when {
            state.isLoading -> RecapLoading()
            state.error != null -> RecapError(
                message = state.error.asString(),
                onRetry = { onAction(RecapAction.OnRetryClick) }
            )
            state.recap != null -> RecapStories(
                recap = state.recap,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun RecapLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF1DB954))
        Text(
            text = stringResource(Res.string.recap_loading),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun RecapError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.recap_load_failed),
            color = Color.White,
            fontSize = 22.sp
        )
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(Res.string.recap_retry))
        }
    }
}

@Preview
@Composable
private fun RecapScreenLoadingPreview() {
    RecapScreen(
        state = RecapState(isLoading = true),
        onAction = {},
        onClose = {}
    )
}

// The recap story pages in display order. Pages without data are skipped when building the list.
private enum class RecapPageKind {
    INTRO, SENT, TYPING, RHYTHM, RECEIVED, TOP_CONTACTS, REACTIONS,
    SOCIAL, GROUPS, LEADERBOARD, MAP, GAMES, BETA_TESTER, PASSWORD_RESET, OUTRO
}

private fun buildPages(recap: RecapUi): List<RecapPageKind> = buildList {
    add(RecapPageKind.INTRO)
    add(RecapPageKind.SENT)
    if (recap.charactersTyped > 0) add(RecapPageKind.TYPING)
    if (recap.messagesSent > 0) add(RecapPageKind.RHYTHM)
    add(RecapPageKind.RECEIVED)
    if (recap.topPartners.isNotEmpty()) add(RecapPageKind.TOP_CONTACTS)
    if (recap.reactionsGiven > 0 || recap.reactionsReceived > 0) add(RecapPageKind.REACTIONS)
    add(RecapPageKind.SOCIAL)
    if (recap.groupsMemberOf > 0) add(RecapPageKind.GROUPS)
    if (recap.myRank != null || recap.leaderboardTop.isNotEmpty()) add(RecapPageKind.LEADERBOARD)
    if (recap.mapEntriesCreated > 0 || recap.mapEntriesCreatedAllTime > 0) add(RecapPageKind.MAP)
    if (recap.games.isNotEmpty()) add(RecapPageKind.GAMES)
    add(RecapPageKind.BETA_TESTER)
    if (recap.passwordResetEmailsSentAllTime > 0) add(RecapPageKind.PASSWORD_RESET)
    add(RecapPageKind.OUTRO)
}

@Composable
private fun RecapStories(
    recap: RecapUi,
    onClose: () -> Unit
) {
    val pages = remember(recap) { buildPages(recap) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // Story-style auto-advance: fills the current segment, then moves to the next page.
    // isPaused is read every frame (not used as a LaunchedEffect key) so a press-and-hold
    // can freeze the fill in place without restarting or racing this effect.
    var isPaused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pagerState.currentPage) {
        progress.snapTo(0f)
        var elapsedMillis = 0L
        var lastFrameNanos = -1L
        while (elapsedMillis < PAGE_AUTO_ADVANCE_MILLIS) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos >= 0 && !isPaused) {
                elapsedMillis += (frameNanos - lastFrameNanos) / 1_000_000
                progress.snapTo((elapsedMillis.toFloat() / PAGE_AUTO_ADVANCE_MILLIS).coerceIn(0f, 1f))
            }
            lastFrameNanos = frameNanos
        }
        val nextPage = pagerState.currentPage + 1
        if (nextPage <= pages.lastIndex) {
            // Scroll in the screen scope: currentPage flips mid-scroll, which restarts
            // this effect — running the animation here would cancel it halfway.
            scope.launch { pagerState.animateScrollToPage(nextPage) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Tap zones: left third goes back, the rest advances (closes on the last page).
            // A press-and-hold only pauses the story (like Instagram/Snapchat) instead of
            // navigating: onLongPress suppresses the onTap that would otherwise fire on release.
            // Lives on the parent so pager swipes and page scrolling keep working.
            .pointerInput(pages.size) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onLongPress = { /* no-op: presence alone suppresses onTap on release */ },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        } else {
                            if (pagerState.currentPage < pages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                onClose()
                            }
                        }
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset =
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val distance = pageOffset.absoluteValue.coerceAtMost(1f)
                        alpha = 1f - distance * 0.35f
                        val scale = 1f - distance * 0.08f
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                val visible = pagerState.currentPage == page
                when (pages[page]) {
                    RecapPageKind.INTRO -> RecapIntroPage(recap, visible)
                    RecapPageKind.SENT -> RecapMessagesSentPage(recap, visible)
                    RecapPageKind.TYPING -> RecapTypingPage(recap, visible)
                    RecapPageKind.RHYTHM -> RecapRhythmPage(recap, visible)
                    RecapPageKind.RECEIVED -> RecapMessagesReceivedPage(recap, visible)
                    RecapPageKind.TOP_CONTACTS -> RecapTopContactsPage(recap, visible)
                    RecapPageKind.REACTIONS -> RecapReactionsPage(recap, visible)
                    RecapPageKind.SOCIAL -> RecapSocialPage(recap, visible)
                    RecapPageKind.GROUPS -> RecapGroupsPage(recap, visible)
                    RecapPageKind.LEADERBOARD -> RecapLeaderboardPage(recap, visible)
                    RecapPageKind.MAP -> RecapMapPage(recap, visible)
                    RecapPageKind.GAMES -> RecapGamesPage(recap, visible)
                    RecapPageKind.BETA_TESTER -> RecapBetaTesterPage(recap, visible)
                    RecapPageKind.PASSWORD_RESET -> RecapPasswordResetPage(recap, visible)
                    RecapPageKind.OUTRO -> RecapOutroPage(recap, visible)
                }
            }
        }

        // Story progress indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pages.size) { index ->
                LinearProgressIndicator(
                    progress = {
                        when {
                            pagerState.currentPage > index -> 1f
                            pagerState.currentPage == index -> progress.value
                            else -> 0f
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    drawStopIndicator = {}
                )
            }
        }
    }
}
