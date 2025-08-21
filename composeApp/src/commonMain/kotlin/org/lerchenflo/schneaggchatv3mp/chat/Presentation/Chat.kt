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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.getKoin
import org.lerchenflo.schneaggchatv3mp.sharedUi.DayDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageView
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
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory
    ),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    val sharedViewModel = getKoin().get<SharedViewModel>()
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()

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
                    .weight(1f),
                reverseLayout = true
            ) {
                itemsIndexed(messages) { index, message ->
                    val currentDateMillis = message.message.sendDate?.toLongOrNull()
                    val currentDate = currentDateMillis?.toLocalDate()
                    val nextDate =
                        messages.getOrNull(index + 1)?.message?.sendDate?.toLongOrNull()
                            ?.toLocalDate()



                    MessageView(
                        messagewithreaders = message,
                        modifier = Modifier
                    )
                    // Show divider only if this message starts a new day
                    if (currentDate != nextDate && currentDate != null) {
                        DayDivider(currentDateMillis)
                    }
                }
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

                OutlinedTextField(
                    value = viewModel.sendText,
                    onValueChange = { newValue ->
                        viewModel.updatesendText(newValue)
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

fun Long.toLocalDate(): LocalDate {
    val instant = Instant.fromEpochMilliseconds(this)
    return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
}
