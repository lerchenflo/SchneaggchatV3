package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.games.domain.ChatWrappedData

@Composable
fun IntroPage(stats: ChatWrappedData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1033)) // Deep Electric Violet
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "In ${stats.year}, you had a LOT to say.",
                color = Color(0xFF1DB954), // Neon Green
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "You sent",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stats.totalMessages.toString(),
                color = Color(0xFFFF007F), // Hot Pink
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 70.sp
            )
            Text(
                text = "messages to your favorite humans. (And maybe a few group chats you muted).",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}