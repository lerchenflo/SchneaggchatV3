package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

import kotlinx.coroutines.sync.Mutex

/**
 * Dedup state for [org.lerchenflo.schneaggchatv3mp.datasource.domain.MessageRepository.markChatReadInBackground].
 * One instance per chat screen, so it lives exactly as long as that screen did before it moved
 * out of ChatViewModel; the requests themselves run in the app-wide scope and outlive it.
 */
class MarkReadGuard {

    // Serialises the requests instead of dropping overlapping ones: a message arriving while a
    // request is in flight would otherwise be swallowed by the guard and then left unread, because
    // the in-flight run finishes by advancing lastMarkedReadMessageId past it. Queued callers
    // re-check under the lock, so the redundant ones cost nothing.
    internal val mutex = Mutex()

    // Guards the mark-read: without this it re-runs (and hits the network) on every single
    // messages flow emission, including ones with nothing unread - which then re-emits the
    // messages flow and reprocesses the whole list again.
    //
    // Only set once the server has actually accepted the read. A failed or lost attempt leaves it
    // null so the next emission retries - the read state itself lives on the server now, so a
    // chat can legitimately come back unread after we already asked once.
    var lastMarkedReadMessageId: String? = null
        internal set
}
