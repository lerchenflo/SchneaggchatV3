package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.games.domain.TopContact

@Composable
fun TopContactsPage(contacts: List<TopContact>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFDF00)) // Bright Neon Yellow
            .padding(horizontal = 24.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Your Inner Circle.",
            color = Color.Black,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 46.sp
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            contacts.forEachIndexed { index, contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contact.dynamicColor)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1} ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = contact.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "${contact.totalMessages} texts",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text(
            text = "You're a loyal friend. Or codependent. We won't judge.",
            color = Color.Black.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}