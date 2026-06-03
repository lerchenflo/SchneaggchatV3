package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                KoinPlatform.getKoin().get<AppRepository>().setNotificationToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val decoded = PayloadDecoder.decode(message.data) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val languageService = KoinPlatform.getKoin().get<LanguageService>()
                languageService.applyLanguage(languageService.getCurrentLanguage())

                val notifier = KoinPlatform.getKoin().get<Notifier>()
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()

                val encryptionKey = prefs.getEncryptionKey().ifEmpty { null }
                val content = resolveLocalizedContent(decoded, encryptionKey) ?: return@launch

                val suppressNotification = AppLifecycleManager.isAppInForeground
                    && decoded is DecodedNotification.Message //Suppress only messages
                    && decoded.senderId.isNotEmpty() && decoded.receiverId.isNotEmpty()
                    && run {
                        val currentChat = KoinPlatform.getKoin().get<GlobalViewModel>().selectedChat.value
                        !currentChat.isNotSelected() //Suppress only when chat is selected
                                && ((currentChat.id == decoded.senderId && decoded.groupMessage == currentChat.isGroup) //Single messages
                                || (currentChat.id == decoded.receiverId && decoded.groupMessage == currentChat.isGroup)) //Group messages
                    }

                if (!suppressNotification) {
                    notifier.showLocalNotification(content)
                }

                if (decoded is DecodedNotification.Message) {
                    val appRepository = KoinPlatform.getKoin().get<AppRepository>()
                    SessionCache.login(tokens = prefs.getTokens(), developer = false)
                    appRepository.messageIdSync()
                }
            }.onFailure { e ->
                println("[AppFirebaseMessagingService] Error handling push: ${e.message}")
            }
        }
    }
}
