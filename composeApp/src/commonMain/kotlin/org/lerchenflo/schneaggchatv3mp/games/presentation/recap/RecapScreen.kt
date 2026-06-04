package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.games.domain.ChatWrappedData
import org.lerchenflo.schneaggchatv3mp.games.domain.TopContact
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.EmojiPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.IntroPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.NightOwlPage
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages.TopContactsPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecapScreen(
    onBackClick: () -> Unit,
) {
    val mockWrappedData = ChatWrappedData(
        totalMessages = 48291,
        totalHoursVoice = 142,
        lateNightPercentage = 64,
        topEmojis = listOf("😭", "💀", "🫠", "✨", "🍿"),
        topContacts = listOf(
            TopContact("Sarah", 18432, Color(0xFFFF477E)),
            TopContact("The Group Chat 💬", 12104, Color(0xFF3F37C9)),
            TopContact("Mom", 9432, Color(0xFF4CC9F0)),
            TopContact("Alex", 5231, Color(0xFF7209B7)),
            TopContact("Food Delivery Guy", 3092, Color(0xFFF72585))
        )
    )

// Call this in your setContent {} block!
// ChatWrappedScreen(stats = mockWrappedData)
    val stats = mockWrappedData

    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Deep dark background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // The Sliding Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> IntroPage(stats)
                    1 -> TopContactsPage(stats.topContacts)
                    2 -> NightOwlPage(stats.lateNightPercentage)
                    3 -> EmojiPage(stats.topEmojis)
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