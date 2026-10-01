package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

/** The things a chat can have shared in it, one tab each. */
@Serializable
enum class SharedContentTab {
    IMAGES,
    LINKS,
    MESSAGES
}

data class SharedContentState(
    val images: List<SharedImageItem> = emptyList(),
    val links: List<SharedLinkItem> = emptyList(),
    val messages: List<SharedMessageItem> = emptyList(),
    //Which message type the MESSAGES tab lists
    val messageType: MessageType = MessageType.POLL,
    val isLoadingMessages: Boolean = true,
    val selectedTab: SharedContentTab = SharedContentTab.IMAGES,
    val isLoading: Boolean = true,
)

sealed interface SharedContentAction {
    data class OnTabSelected(val tab: SharedContentTab) : SharedContentAction
    data class OnMessageTypeSelected(val type: MessageType) : SharedContentAction
    data class OnGoToMessageClick(val messageId: String) : SharedContentAction
    data class OnCopyLinkClick(val url: String) : SharedContentAction
    data class OnDownloadImageClick(val item: SharedImageItem) : SharedContentAction
    data object OnBackClick : SharedContentAction
}
