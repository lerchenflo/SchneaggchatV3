package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person3
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.sharedUi.DayDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.groups
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.persons
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import schneaggchatv3mp.composeapp.generated.resources.unread
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true
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
                var addMediaDropdownExpanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = {addMediaDropdownExpanded = true},
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = stringResource(Res.string.add),

                    )
                }
                if(addMediaDropdownExpanded){
                    DropdownMenu(
                        expanded = addMediaDropdownExpanded,
                        onDismissRequest = { addMediaDropdownExpanded = false }
                    ) {
                        AddMediaOptions.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row{
                                        Icon(
                                            imageVector = option.getIcon(),
                                            contentDescription = option.toUiText().asString(),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = option.toUiText().asString()
                                        )
                                    }
                                },
                                onClick = {
                                    addMediaDropdownExpanded = false
                                    option.getAction()
                                }
                            )
                        }
                    }
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

enum class AddMediaOptions{
    IMAGE,
    POLL,
    AUDIO;

    fun toUiText(): UiText = when (this) {
        IMAGE -> UiText.StringResourceText(Res.string.image)
        POLL -> UiText.StringResourceText(Res.string.poll)
        AUDIO   -> UiText.StringResourceText(Res.string.audio)
        else -> UiText.StringResourceText(Res.string.unknown_error)
    }
    fun getIcon(): ImageVector = when (this) {
        IMAGE -> Icons.Default.Image
        POLL -> Icons.Default.Poll
        AUDIO   -> Icons.Default.Headphones
        else -> Icons.Default.Menu
    }

    fun getAction(): Unit = when (this) {
        IMAGE -> {
            SnackbarManager.showMessage("to be done (image)")
        }
        POLL -> {
            SnackbarManager.showMessage("to be done (poll)")
        }
        AUDIO   -> {
            SnackbarManager.showMessage("to be done (audio)")
        }
        else -> {
            // todo flo tua din cooles error popup do ine
            println("unknown action called")
        }
    }
}