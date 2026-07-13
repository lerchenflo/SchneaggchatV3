package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks which chat is currently visible on screen so code outside the UI (e.g. the socket
 * message handler) can decide whether to show a notification for an incoming message.
 * Set/cleared by the chat screen; everything else only reads it.
 */
object OpenChatTracker {

    data class OpenChat(val chatId: String, val isGroup: Boolean)

    private val _current = MutableStateFlow<OpenChat?>(null)
    val current = _current.asStateFlow()

    fun onChatOpened(chatId: String, isGroup: Boolean) {
        _current.value = OpenChat(chatId, isGroup)
    }

    /** Only clears if this chat is still the visible one (guards against open/close race when switching chats). */
    fun onChatClosed(chatId: String, isGroup: Boolean) {
        if (_current.value == OpenChat(chatId, isGroup)) {
            _current.value = null
        }
    }

    fun isChatOpen(chatId: String, isGroup: Boolean): Boolean {
        return _current.value == OpenChat(chatId, isGroup)
    }
}
