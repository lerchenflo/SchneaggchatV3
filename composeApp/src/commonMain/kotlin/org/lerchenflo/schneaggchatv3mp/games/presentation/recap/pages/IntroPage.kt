package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapData
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_using_schneaggchat_in_numbers
import schneaggchatv3mp.composeapp.generated.resources.recap_your_year


@OptIn(ExperimentalMaterial3Api::class)
@Preview()
@Composable
fun IntroPage(stats: Flow<RecapData>) {

    val year = stats.collectAsState(null).value?.year ?: 2000

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff0a1ed3)) // Deep Electric Violet
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(Res.string.recap_your_year),
                color = Color(0xFF1DB954), // Neon Green
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = year.toString(),
                color = Color(0xffd63cff),
                fontSize = 60.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(Res.string.recap_using_schneaggchat_in_numbers),
                color = Color(0xFF1DB954), // Neon Green
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp
            )
        }
    }
}