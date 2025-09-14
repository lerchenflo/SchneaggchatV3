package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme

@Composable
fun ChipSelection(
    chips: List<String>,
    onSelected: (Int) -> Unit
){
    var selectedChipIndex by remember {
        mutableStateOf(0)
    }

    LazyRow {
        items(chips.size)
        {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(
                        start = 15.dp,
                        top = 15.dp,
                        bottom = 15.dp
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .clickable{
                        selectedChipIndex = it
                        onSelected(it)
                    }
                    .background(
                        if (selectedChipIndex == it) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .padding(15.dp)
            ){
                Text(
                    text = chips[it],
                )
            }
        }
    }
}


@Composable
@Preview(showBackground = true)
private fun ChipSelectionPreview(){
    SchneaggchatTheme {
        ChipSelection(
            listOf("Fortnite", "GTA", "Schneaggchat"),
            {}
        )
    }

}