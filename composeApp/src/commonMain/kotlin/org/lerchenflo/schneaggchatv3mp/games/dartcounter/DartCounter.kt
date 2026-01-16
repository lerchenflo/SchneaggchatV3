package org.lerchenflo.schneaggchatv3mp.games.dartcounter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun DartCounter() {

    val viewmodel = koinInject<DartCounterViewmodel>()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)

    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
        ) {
            items(
                count = 20,
            ) {

                if (it % 2 == 0) {
                    Button(
                        onClick = {

                        },
                    ) {
                        Text(
                            text = it.toString()
                        )
                    }
                } else {
                    Button(
                        onClick = {

                        },
                        
                        modifier = Modifier
                    ) {
                        Text(
                            text = it.toString()
                        )
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = viewmodel.count.toString()
        )

    }


}