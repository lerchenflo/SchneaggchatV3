package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.flow.MutableStateFlow

object IosPushTokenStore {
    val token = MutableStateFlow<String?>(null)

    fun onApnsToken(hexToken: String) {
        token.value = hexToken
    }

    fun clear() {
        token.value = null
    }
}
