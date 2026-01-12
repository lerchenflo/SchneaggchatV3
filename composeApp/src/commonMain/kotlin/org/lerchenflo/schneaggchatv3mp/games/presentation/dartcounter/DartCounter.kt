package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.ViewModel
import org.koin.compose.koinInject

@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
fun DartCounter() {
    val viewmodel = koinInject<DartCounterViewModel>()
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ){

        Button(
            onClick = {

            }
        ) {
            Text("Add Players")
        }

        Button(
            onClick = {

            }
        ) {
            Text("Start Game")
        }

        Button(
            onClick = {

            }
        ) {
            Text("Highscores")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(45.dp)

    ) {
        LazyVerticalGrid( columns = GridCells.Adaptive(80.dp),
        ) {
            items(
                count = 23,
            ) {
                Button(modifier = Modifier
                    .padding(2.dp),

                    onClick = {
                        if (it==21){
                            viewmodel.count += 25;
                        }else if (it==22){
                            viewmodel.count += 50;
                        }else{
                            viewmodel.count += it;
                        }

                    },
                ) {
                    if (it == 0) {
                        Text(
                            text = "Out"
                        )
                    } else if (it == 21) {
                        Text(
                            text = "25"
                        )
                    } else if (it == 22) {
                        Text(
                            text = "50"
                        )
                    }else{
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