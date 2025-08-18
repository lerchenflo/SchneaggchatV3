package org.lerchenflo.schneaggchatv3mp.chatauswahl.Presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.database.User

@Composable
fun Chatauswahlscreen(modifier: Modifier = Modifier) {

    val viewModel = koinViewModel<SharedViewModel>()

    val users by viewModel.getAllUsers().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        val userlist = listOf<User>(
            User(1, 0, "Ben"),
            User(2, 0, "David"),
            User(3, 0, "flo"),
            User(4, 0, "fabi")
        )

        userlist.forEach {
            viewModel.upsertUser(it)
        }
    }

    //Hauptlayout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()

    ) {
        //Obere Zeile für Buttons
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                fontSize = 26.sp,
                text = "SchneaggchatV3",
                modifier = Modifier
                    .padding(16.dp)


            )

            //TODO: Schneaggmap button usw
        }

        //Zweite Zeile für Freund suchen
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {
                    //TODO: Suchen
                }
            )

            //TODO: Filtersymbol

            Button(

                onClick = {  },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Neuer Chat"
                )
            }
        }

        //Gegneranzeige
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(users) { user ->
                AddUserButton(user)
                HorizontalDivider(
                    thickness = 2.dp
                )
            }
        }
    }
}



@Preview
@Composable
fun AddUserButton(user: User?){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable{
                //openChat(user?)
            }
    ){
        Text(
            text = user?.name.toString(),
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
