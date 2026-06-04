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

@Composable
fun NightOwlPage(lateNightPercentage: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191414)) // Pitch Black
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🌙",
                fontSize = 72.sp
            )
            Text(
                text = "The Ultimate Night Owl",
                color = Color(0xFFBB86FC), // Pastel Purple
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$lateNightPercentage%",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "of your messages were sent past 11:00 PM. Go to sleep. The gossip will still be there tomorrow.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}