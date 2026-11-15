package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.copyToClipboard
import org.lerchenflo.schneaggchatv3mp.utilities.extractLinks

/**
 * Backs the shared content screen: every image and every link ever shared in one chat, kept up to
 * date from the database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedContentViewModel(
    private val messageRepository: MessageRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val navigator: Navigator,
    private val pictureManager: PictureManager,

    private val chatId: String,
    private val isGroup: Boolean,
    initialTab: SharedContentTab,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SharedContentState(
            selectedTab = initialTab
        )
    )
    val state = _state.asStateFlow()

    /** Names of the users [messages] show (senders, system event people), kept up to date. */
    private fun namesOf(messages: List<Message>): Flow<Map<String, UserIdentityName>> =
        userIdentityRepository.observeUsers(messages.flatMapTo(mutableSetOf()) { it.previewUserIds() + it.senderId })
            .map { users -> users.mapValues { (_, user) -> user.name } }

    init {
        viewModelScope.launch {
            combine(
                messageRepository.getImageMessagesForChatFlow(chatId, isGroup),
                messageRepository.getLinkCandidateMessagesForChatFlow(chatId, isGroup),
            ) { imageMessages, linkCandidates -> imageMessages to linkCandidates }
                .flatMapLatest { (imageMessages, linkCandidates) ->
                    namesOf(imageMessages + linkCandidates).map { names -> Triple(imageMessages, linkCandidates, names) }
                }
                .map { (imageMessages, linkCandidates, senderNames) ->
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

        // The MESSAGES tab follows whichever type is picked in its dropdown
        viewModelScope.launch {
            _state
                .map { it.messageType }
                .distinctUntilChanged()
                .flatMapLatest { type ->
                    _state.update { it.copy(isLoadingMessages = true) }
                    messageRepository.getMessagesOfTypeForChatFlow(chatId, isGroup, type)
                        .flatMapLatest { messages ->
                            namesOf(messages).map { names -> messages.map { message -> message.toSharedMessageItem(names) } }
                        }
                }
                .flowOn(Dispatchers.Default)
                .collectLatest { messages ->
                    _state.update { it.copy(messages = messages, isLoadingMessages = false) }
                }
        }
    }

    private fun Message.toSharedMessageItem(senderNames: Map<String, UserIdentityName>): SharedMessageItem {
        val poll = poll
        return SharedMessageItem(
            messageId = id,
            localPK = localPK,
            type = msgType,
            text = poll?.title ?: content,
            pictureUrl = pictureUrl,
            systemEvent = systemEvent,
            systemEventNames = systemEvent?.userIds()?.associateWith { senderNames[it] ?: UserIdentityName.Unknown }.orEmpty(),
            senderName = senderName(senderNames),
            sendDate = getSendDateAsLong(),
            poll = poll?.let {
                SharedPollInfo(
                    voterCount = it.getUniqueVoterCount(),
                    subPollCount = it.subPollCount(),
                    isClosed = it.isExpired(),
                )
            },
        )
    }

    private fun Message.senderName(senderNames: Map<String, UserIdentityName>): UserIdentityName =
        senderNames[senderId] ?: UserIdentityName.Unknown

    fun onAction(action: SharedContentAction) {
        when (action) {
            is SharedContentAction.OnTabSelected -> _state.update { it.copy(selectedTab = action.tab) }
            is SharedContentAction.OnMessageTypeSelected -> _state.update { it.copy(messageType = action.type) }
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
