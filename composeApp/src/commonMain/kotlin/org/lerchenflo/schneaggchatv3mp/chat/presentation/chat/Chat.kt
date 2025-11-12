package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.DayDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.message
import kotlin.time.ExperimentalTime

@Preview()
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onChatDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    val globalViewModel = koinInject<GlobalViewModel>()
    val viewModel = koinViewModel<ChatViewModel>()
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()

    //TODO Fabi leas: Immer a launchedeffect verwenda sunsch wirds bei jeder recomposition ufgrufa (Denkt da chatbot mol)
    LaunchedEffect(true){
        viewModel.initPrefs()
    }

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
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserButton(
                        selectedChat = globalViewModel.selectedChat.value,
                        onClickGes = {
                            onChatDetailsClick()
                        },
                    )
                }
            }


            // Messages
            val listState = rememberLazyListState()

            //Wenn sich die letzt nachricht ändert denn abescrollen TODO: Testa ob des automatisch abescrollt
            if (messages.isNotEmpty()){
                LaunchedEffect(messages.first()) {
                    listState.animateScrollToItem(0, scrollOffset = 2)
                }
            }

            //TODO: Lag fixen do isch iwas falsch
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true,
                state = listState
            ) {
                itemsIndexed(messages, key = { _, msg ->
                    // combine id + sendDate (or use a uuid field) so duplicates don't collide
                    "${msg.messageDto.localPK}_${msg.messageDto.sendDate}"
                })  { index, message ->
                    val currentDateMillis = message.messageDto.sendDate.toLongOrNull()
                    val currentDate = currentDateMillis?.toLocalDate()
                    val nextDate =
                        messages.getOrNull(index + 1)?.messageDto?.sendDate?.toLongOrNull()
                            ?.toLocalDate()



                    MessageView(
                        useMD = viewModel.markdownEnabled,
                        selectedChatId = globalViewModel.selectedChat.value.id,
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
                    onClick = {viewModel.sendMessage()},
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(Res.string.add),
                    )
                }

            }

        }

    }
}

@OptIn(ExperimentalTime::class)
fun Long.toLocalDate(): LocalDate {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(this)
    return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
}
