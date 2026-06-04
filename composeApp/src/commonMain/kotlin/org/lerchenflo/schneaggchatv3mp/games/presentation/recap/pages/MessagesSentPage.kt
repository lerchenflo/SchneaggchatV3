package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapData
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_in_year_you_had_a_lot_to_say
import schneaggchatv3mp.composeapp.generated.resources.recap_messages_to_favorite_humans
import schneaggchatv3mp.composeapp.generated.resources.recap_you_sent

@Composable
fun MessagesSentPage(statsFlow: Flow<RecapData>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1033)) // Deep Electric Violet
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        //val totalMessagesSent by stats.totalMessagesSent.collectAsState(initial = 0)
        val stats = statsFlow.collectAsState(null).value

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(Res.string.recap_in_year_you_had_a_lot_to_say, stats?.year ?: 0),
                color = Color(0xFF1DB954), // Neon Green
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(Res.string.recap_you_sent),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stats?.totalMessagesSent.toString(),
                color = Color(0xFFFF007F), // Hot Pink
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 70.sp
            )
            Text(
                text = stringResource(Res.string.recap_messages_to_favorite_humans),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}