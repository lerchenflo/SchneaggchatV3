package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.RecapMapLeaderboardPage
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
import schneaggchatv3mp.composeapp.generated.resources.recap_close
import schneaggchatv3mp.composeapp.generated.resources.recap_hold_hint
import schneaggchatv3mp.composeapp.generated.resources.recap_load_failed
import schneaggchatv3mp.composeapp.generated.resources.recap_loading
import schneaggchatv3mp.composeapp.generated.resources.recap_retry
import kotlin.math.absoluteValue

private const val PAGE_AUTO_ADVANCE_MILLIS = 12_000L

/** Taps left of this fraction of the width go back; everything else advances. */
private const val BACK_TAP_ZONE_FRACTION = 0.35f

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

/**
 * The recap is a self-contained branded story and paints its own colors on purpose
 * (see [RecapPalette]) - it must look the same in light and dark theme.
 */
@Composable
fun RecapScreen(
    state: RecapState,
    onAction: (RecapAction) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RecapPalette.Ink
    ) {
        when {
            state.isLoading -> RecapLoading(onClose = onClose)
            state.error != null -> RecapError(
                message = state.error.asString(),
                onRetry = { onAction(RecapAction.OnRetryClick) },
                onClose = onClose
            )
            state.recap != null -> RecapStories(
                recap = state.recap,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun RecapLoading(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = RecapPalette.Green)
            Text(
                text = stringResource(Res.string.recap_loading),
                color = RecapPalette.Paper.copy(alpha = 0.8f),
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        RecapCloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun RecapError(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.recap_load_failed),
                color = RecapPalette.Paper,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = RecapPalette.Paper.copy(alpha = 0.6f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecapPalette.Green,
                    contentColor = RecapPalette.Ink
                )
            ) {
                Text(text = stringResource(Res.string.recap_retry), fontWeight = FontWeight.Bold)
            }
        }
        RecapCloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** Close control shared by every recap state, kept clear of the status bar / notch. */
@Composable
private fun RecapCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClose,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 28.dp, end = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(RecapPalette.Ink.copy(alpha = 0.35f))
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(Res.string.recap_close),
            tint = RecapPalette.Paper
        )
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
    SOCIAL, GROUPS, LEADERBOARD, MAP, MAP_LEADERBOARD, GAMES, BETA_TESTER, PASSWORD_RESET, OUTRO
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
    if (recap.mapEntriesCreated > 0 || recap.mapEntriesEdited > 0 || recap.mapEntriesCreatedAllTime > 0) add(RecapPageKind.MAP)
    if (recap.myMapRank != null || recap.mapLeaderboardTop.isNotEmpty()) add(RecapPageKind.MAP_LEADERBOARD)
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
    // can freeze the fill in place without restarting or racing this effect. The timer also
    // pauses while the user is mid-swipe so a half-dragged page never auto-jumps under the finger.
    var isPaused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pagerState.settledPage) {
        progress.snapTo(0f)
        var elapsedMillis = 0L
        var lastFrameNanos = -1L
        while (elapsedMillis < PAGE_AUTO_ADVANCE_MILLIS) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos >= 0 && !isPaused && !pagerState.isScrollInProgress) {
                elapsedMillis += (frameNanos - lastFrameNanos) / 1_000_000
                progress.snapTo((elapsedMillis.toFloat() / PAGE_AUTO_ADVANCE_MILLIS).coerceIn(0f, 1f))
            }
            lastFrameNanos = frameNanos
        }
        val nextPage = pagerState.settledPage + 1
        if (nextPage <= pages.lastIndex) {
            // Scroll in the screen scope: settledPage flips at the end of the scroll, which restarts
            // this effect - running the animation here would cancel it halfway.
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
                        val current = pagerState.currentPage
                        if (offset.x < size.width * BACK_TAP_ZONE_FRACTION) {
                            if (current > 0) {
                                scope.launch { pagerState.animateScrollToPage(current - 1) }
                            }
                        } else {
                            if (current < pages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(current + 1) }
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
            // Keep both neighbours composed so a swipe never reveals a blank page for a frame.
            beyondViewportPageCount = 1,
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
                // Animations start once the page has settled, not while it is still sliding in.
                val visible = pagerState.settledPage == page
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
                    RecapPageKind.MAP_LEADERBOARD -> RecapMapLeaderboardPage(recap, visible)
                    RecapPageKind.GAMES -> RecapGamesPage(recap, visible)
                    RecapPageKind.BETA_TESTER -> RecapBetaTesterPage(recap, visible)
                    RecapPageKind.PASSWORD_RESET -> RecapPasswordResetPage(recap, visible)
                    RecapPageKind.OUTRO -> RecapOutroPage(recap, visible)
                }
            }
        }

        // Story chrome: segmented progress + close button, below the status bar / notch and
        // never wider than the content column so it lines up with the pages on desktop.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(pages.size) { index ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                pagerState.settledPage > index -> 1f
                                pagerState.settledPage == index -> progress.value
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = RecapPalette.Paper,
                        trackColor = RecapPalette.Paper.copy(alpha = 0.3f),
                        drawStopIndicator = {},
                        gapSize = 0.dp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.recap_hold_hint),
                    color = RecapPalette.Paper.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RecapPalette.Ink.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.recap_close),
                        tint = RecapPalette.Paper,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
