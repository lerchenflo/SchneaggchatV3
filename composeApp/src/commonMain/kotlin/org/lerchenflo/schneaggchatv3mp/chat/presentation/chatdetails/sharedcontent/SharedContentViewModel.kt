package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.copyToClipboard
import org.lerchenflo.schneaggchatv3mp.utilities.extractLinks

/**
 * Backs the shared content screen: every image and every link ever shared in one chat, kept up to
 * date from the database.
 */
class SharedContentViewModel(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
    private val pictureManager: PictureManager,

    private val chatId: String,
    private val isGroup: Boolean,
    showLinks: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SharedContentState(
            selectedTab = if (showLinks) SharedContentTab.LINKS else SharedContentTab.IMAGES
        )
    )
    val state = _state.asStateFlow()

    // Only the id -> displayName projection, not the full User list: getAllUsersFlow() emits on ANY
    // row change in `users` - including the presence writes that arrive constantly over the socket
    // and have nothing to do with this chat. Narrowing it first means only a real name change
    // rebuilds the lists.
    private val senderNamesFlow: Flow<Map<String, String>> = userRepository.getAllUsersFlow()
        .map { users -> users.associate { it.id to it.displayName } }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            combine(
                messageRepository.getImageMessagesForChatFlow(chatId, isGroup),
                messageRepository.getLinkCandidateMessagesForChatFlow(chatId, isGroup),
                senderNamesFlow
            ) { imageMessages, linkCandidates, senderNames ->
                val images = imageMessages
                    .filter { !it.pictureUrl.isNullOrEmpty() }
                    .map { message ->
                        SharedImageItem(
                            messageId = message.id,
                            pictureUrl = message.pictureUrl.orEmpty(),
                            caption = message.content,
                            senderName = message.senderName(senderNames),
                            sendDate = message.getSendDateAsLong(),
                        )
                    }

                // One row per url - a message with two links belongs in the list twice
                val links = linkCandidates.flatMap { message ->
                    extractLinks(message.content).map { url ->
                        SharedLinkItem(
                            messageId = message.id,
                            url = url,
                            senderName = message.senderName(senderNames),
                            sendDate = message.getSendDateAsLong(),
                        )
                    }
                }

                images to links
            }
                // The link regex runs over every candidate message of the chat, so keep it off the
                // main thread
                .flowOn(Dispatchers.Default)
                .collectLatest { (images, links) ->
                    _state.update {
                        it.copy(images = images, links = links, isLoading = false)
                    }
                }
        }
    }

    /** Resolved display name of the sender, falling back to whatever the message itself carries. */
    private fun Message.senderName(senderNames: Map<String, String>): String =
        senderNames[senderId] ?: senderAsString

    fun onAction(action: SharedContentAction) {
        when (action) {
            is SharedContentAction.OnTabSelected -> _state.update { it.copy(selectedTab = action.tab) }
            is SharedContentAction.OnGoToMessageClick -> goToMessage(action.messageId)
            is SharedContentAction.OnCopyLinkClick -> copyToClipboard(action.url)
            is SharedContentAction.OnDownloadImageClick -> downloadImage(action.item)
            SharedContentAction.OnBackClick -> onBackClick()
        }
    }

    /**
     * Open the chat scrolled to the message this item came from. The details screen this was opened
     * from stays on the backstack on purpose - going back from the chat returns there, the same way
     * a message search result behaves.
     */
    private fun goToMessage(messageId: String) {
        viewModelScope.launch {
            navigator.navigate(
                Route.Chat(chatId = chatId, isGroup = isGroup, highlightMessageId = messageId)
            )
        }
    }

    private fun downloadImage(item: SharedImageItem) {
        viewModelScope.launch {
            pictureManager.downloadImage(
                item.pictureUrl,
                "Schneaggchat_${item.sendDate}.jpeg"
            )
        }
    }

    private fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }
}
