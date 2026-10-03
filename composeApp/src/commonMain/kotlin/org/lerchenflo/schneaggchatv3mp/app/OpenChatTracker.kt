package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tracks which chat is currently visible on screen so code outside the UI (e.g. the socket
 * message handler) can decide whether to show a notification for an incoming message.
 * Set/cleared by the chat screen; everything else only reads it.
 */
object OpenChatTracker {

    /**
     * How long a message has to stay on screen in the open chat before it counts as seen.
     * Leaving the app (home swipe, app switch) keeps the activity RESUMED until the gesture
     * finishes, so a message landing in that window used to be silenced and marked read
     * although the user never saw it. Notification suppression and the auto-read both wait
     * this long and then re-check [isSeen].
     */
    val SEEN_CONFIRM_DELAY = 1500.milliseconds

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

    /** The chat is on screen right now: open and the app in the foreground. */
    fun isSeen(chatId: String, isGroup: Boolean): Boolean {
        return AppLifecycleManager.isAppInForeground && isChatOpen(chatId, isGroup)
    }

    /**
     * Whether a message for this chat arriving now gets seen by the user: the chat is on screen
     * now and still is after [SEEN_CONFIRM_DELAY]. Suspends for that delay only when the chat is
     * on screen; returns false immediately otherwise.
     */
    suspend fun isSeenAfterConfirmDelay(chatId: String, isGroup: Boolean): Boolean {
        if (!isSeen(chatId, isGroup)) return false
        delay(SEEN_CONFIRM_DELAY)
        return isSeen(chatId, isGroup)
    }
}
