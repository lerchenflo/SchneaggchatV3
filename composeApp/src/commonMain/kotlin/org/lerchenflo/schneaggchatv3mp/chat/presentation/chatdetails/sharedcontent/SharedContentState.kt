package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import kotlinx.serialization.Serializable

/** The things a chat can have shared in it, one tab each. */
@Serializable
enum class SharedContentTab {
    IMAGES,
    LINKS,
    POLLS
}

data class SharedContentState(
    val images: List<SharedImageItem> = emptyList(),
    val links: List<SharedLinkItem> = emptyList(),
    val polls: List<SharedPollItem> = emptyList(),
    val selectedTab: SharedContentTab = SharedContentTab.IMAGES,
    val isLoading: Boolean = true,
)

sealed interface SharedContentAction {
    data class OnTabSelected(val tab: SharedContentTab) : SharedContentAction
    data class OnGoToMessageClick(val messageId: String) : SharedContentAction
    data class OnCopyLinkClick(val url: String) : SharedContentAction
    data class OnDownloadImageClick(val item: SharedImageItem) : SharedContentAction
    data object OnBackClick : SharedContentAction
}
