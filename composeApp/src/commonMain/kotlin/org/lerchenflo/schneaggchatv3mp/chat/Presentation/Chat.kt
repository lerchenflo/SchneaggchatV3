package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.message

@Preview
@Composable
fun ChatScreen(
    sharedViewModel: SharedViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    SchneaggchatTheme{ // theme wida setza
        Column(
            modifier = modifier
        ){
            // Obere Zeile (Backbutton, profilbild, name, ...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp, 10.dp, 16.dp)
            ){

                // Backbutton
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.go_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserButton(
                        user = sharedViewModel.selectedChat.value,
                        onClickGes = {
                            // todo open chatdetails
                            SnackbarManager.showMessage("Bald chatdetails")
                        }
                    )
                }
            }
            // Messages
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)

            ) {
                // todo messages Printen
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp) // optional spacing
                    .navigationBarsPadding()
            ){
                // button zum züg addden
                IconButton(
                    onClick = {/*todo*/},
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = stringResource(Res.string.add),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Text field for message
                var text by remember { mutableStateOf("") } // todo text ins viewmodel
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                            newValue -> text = newValue
                        /* TODO: Suchen */
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.message) + " ...") }
                )

                // send button
                IconButton(
                    onClick = {/*todo*/},
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(Res.string.add),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

            }

        }

    }
}