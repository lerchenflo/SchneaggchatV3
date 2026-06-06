package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapData
import org.lerchenflo.schneaggchatv3mp.games.domain.TopContact
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_texts_number
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

@Composable
fun TopContactsPage(statsFlow: Flow<RecapData>) {

    val stats = statsFlow.collectAsState(null).value

    val contacts = stats?.topContacts

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

        val colors = listOf(
            Color(0xFF4D00FF), // Electric Indigo
            Color(0xFF0066FF), // Vivid Blue
            Color(0xFF9E00FF), // Deep Violet
            Color(0xFF007A5E), // Forest Teal
            Color(0xFFD00056)  // Raspberry Red
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            contacts?.forEachIndexed { index, contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors[index])
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
                        ProfilePictureView(
                            filepath = contact.selectedChat?.profilePictureUrl ?: "",
                            modifier = Modifier
                                .size(50.dp) // Use square aspect ratio
                                .padding(end = 8.dp) // Right padding only
                                .clip(CircleShape) // Circular image
                        )
                        Text(
                            text = contact.selectedChat?.name ?: stringResource(Res.string.unknown_user),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = stringResource(Res.string.recap_texts_number, contact.msgCount),
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