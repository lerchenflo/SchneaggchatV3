package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_are_now_your_friends
import schneaggchatv3mp.composeapp.generated.resources.recap_new_friends_found
import schneaggchatv3mp.composeapp.generated.resources.recap_you_have

@OptIn(ExperimentalMaterial3Api::class)
@Preview()
@Composable
fun NewFriendsPage(newFriends: Int) {
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
            Icon(imageVector = Icons.Default.GroupAdd,
                contentDescription = "Friendgroup icon",
                modifier = Modifier.size(90.dp),
                tint = Color(0xfff3d023)
            )

            Text(
                text = stringResource(Res.string.recap_new_friends_found),
                color = Color(0xFFBB86FC), // Pastel Purple
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(Res.string.recap_you_have),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Text(
                text = "$newFriends",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(Res.string.recap_are_now_your_friends),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}