package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapData
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.EmojiPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.IntroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.MessagesReceivePage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.MessagesSentPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.NewFriendsPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.TopContactsPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecapScreen(
    onBackClick: () -> Unit,
    recapViewModel: RecapViewModel
) {


// Call this in your setContent {} block!
// ChatWrappedScreen(stats = mockWrappedData)
    val stats = recapViewModel.getstats()

    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Deep dark background
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Removes the default ripple effect for a clean story feel
            ) {
                val nextPage = pagerState.currentPage + 1
                if (nextPage < pageCount) {
                    scope.launch {
                        pagerState.animateScrollToPage(nextPage)
                    }
                } else {
                    // If they tap on the last page, close the recap
                    onBackClick()
                }
            }
        ) {

            // The Sliding Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> IntroPage(stats)
                    1 -> MessagesSentPage(stats)
                    2 -> MessagesReceivePage(stats)
                    3 -> TopContactsPage(stats)
                    // todo mehr stats usdenka, vom server züha und schüa azoaga (mit viel chatbot ui)
                    //2 -> NewFriendsPage(stats.lateNightPercentage)
                    //3 -> EmojiPage(stats.topEmojis)
                }
            }

            // Top Story Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(pageCount) { index ->
                    val progress = when {
                        pagerState.currentPage > index -> 1f
                        pagerState.currentPage == index -> 1f // For a real app, hook this up to a timer anim
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}