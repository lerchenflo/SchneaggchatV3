package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements.ChatInputBar
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements.ChatMessageList
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements.ChatTopBar
import org.lerchenflo.schneaggchatv3mp.sharedUi.clearFocusOnTap
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress

@Composable
fun ChatScreenRoot(
    chatId: String,
    isGroup: Boolean,
    highlightMessageId: String? = null,
) {
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(chatId, isGroup) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        playbackProgress = viewModel.playbackProgress,
        highlightMessageId = highlightMessageId,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    playbackProgress: StateFlow<PlaybackProgress>,
    highlightMessageId: String?,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
){
    //Leave chat when not logged in
    SessionCache.authStateValue // reactive read: recompose once autologin finishes instead of staying blank
    val ownId = SessionCache.requireLoggedIn()?.userId ?: return

    // Track the visible chat so the socket handler can suppress notifications for it
    DisposableEffect(state.chatId, state.isGroup) {
        OpenChatTracker.onChatOpened(state.chatId, state.isGroup)
        onDispose { OpenChatTracker.onChatClosed(state.chatId, state.isGroup) }
    }

    Scaffold(
        modifier = modifier.clearFocusOnTap(),
        // IOSKEYBOARDFIX: the root Scaffold in App.kt already applies systemBars + ime as
        // contentWindowInsets, so this nested Scaffold must not re-derive its own systemBars
        // inset (that used to double-count the bottom inset, which the old code below worked
        // around by only taking calculateTopPadding()). This Scaffold now only contributes the
        // top bar's height.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Obere Zeile (Backbutton, profilbild, name, ...)
            ChatTopBar(
                chatPartner = state.chatPartner,
                ownId = ownId,
                onBackClick = { onAction(ChatAction.OnBackClick) },
                onPartnerClick = { onAction(ChatAction.OnChatDetailsClick) },
            )
        },
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // IOSKEYBOARDFIX: was only calculateTopPadding()
        ) {

            // Messages
            ChatMessageList(
                displayItems = state.displayItems,
                highlightMessageId = highlightMessageId,
                ownId = ownId,
                chatId = state.chatId,
                useMarkdown = state.markdownEnabled,
                playbackProgress = playbackProgress,
                onAction = { onAction(ChatAction.OnMessageAction(it)) },
                modifier = Modifier.weight(1f)
            )

            ChatInputBar(
                sendContent = state.sendContent,
                replyMessage = state.replyMessage,
                replyMessageSender = state.replyMessageSender,
                editMessage = state.editMessage,
                useMarkdown = state.markdownEnabled,
                chatId = state.chatId,
                ownId = ownId,
                isDesktop = state.isDesktop,
                maxVoiceMsgTime = state.maxVoiceMsgTime,
                playbackProgress = playbackProgress,
                onAction = onAction,
            )
        }
    }

}
