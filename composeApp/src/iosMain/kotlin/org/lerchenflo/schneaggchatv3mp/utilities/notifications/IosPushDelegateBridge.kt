package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

class IosPushDelegateBridge {

    fun onTokenReceived(hexToken: String) {
        IosPushTokenStore.saveToken(hexToken)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (SessionCache.authState.value is SessionCache.AuthState.LoggedIn) {
                    KoinPlatform.getKoin().get<AppRepository>().setNotificationToken(hexToken)
                }
            }
        }
    }

    fun onForegroundPayload(data: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                SessionCache.login(tokens = prefs.getTokens(), developer = false)
                KoinPlatform.getKoin().get<AppRepository>().messageIdSync()
            }
        }
    }

    fun onNotificationTap(data: Map<String, String>) {
        // Extract chat info from the push payload for deep-navigation, reusing the same
        // decoder + chat-target logic the Android FCM service uses.
        val decoded = PayloadDecoder.decode(data) as? DecodedNotification.Message

        AppLifecycleManager.notifyNotificationOpened(
            chatId = decoded?.chatTargetId,
            isGroup = decoded?.groupMessage ?: false
        )
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                SessionCache.login(tokens = prefs.getTokens(), developer = false)
                KoinPlatform.getKoin().get<AppRepository>().dataSync(reason = "iosNotificationTap")
            }
        }
    }
}
