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
import schneaggchatv3mp.composeapp.generated.resources.recap_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_others_sent_you

@Composable
fun MessagesReceivePage(statsFlow: Flow<RecapData>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1F2D)) // Midnight Teal
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        //val totalMessagesSent by stats.totalMessagesSent.collectAsState(initial = 0)
        val stats = statsFlow.collectAsState(null).value

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(Res.string.recap_others_sent_you, stats?.year ?: 0),
                color = Color(0xFFF5A623), // Warm Amber
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stats?.totalMessagesReceived.toString(),
                color = Color(0xFFFF5733), // Vivid Coral-Orange
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 70.sp
            )
            Text(
                text = stringResource(Res.string.recap_messages),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}