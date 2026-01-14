package org.lerchenflo.schneaggchatv3mp.utilities

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_group_title
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_single_title
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import kotlin.random.Random

object NotificationManager{


    sealed interface NotificationObject {
        
        data class MessageNotification(
            val msgId: String,
            val senderName: String,
            val groupMessage: Boolean,
            val groupName: String,
            val encodedContent: String
        ) : NotificationObject {
            suspend fun getDecodedContent(key: String): String {
                return CryptoUtil.decrypt(encodedContent, key)
            }
        }

        data class FriendRequestNotification(
            val requesterId: String,
            val requesterName: String
        ) : NotificationObject

        data class SystemNotification(
            val title: String,
            val message: String
        ) : NotificationObject
    }

    private fun PayloadData.toNotificationObject(): NotificationObject? {
        return try {
            // Convert PayloadData (Map) to NotificationResponse using the server's response type
            val notificationResponse = this.toNotificationResponse()
            
            // Convert NotificationResponse to local NotificationObject
            notificationResponse?.toNotificationObject()
        } catch (e: Exception) {
            runBlocking {
                KoinPlatform.getKoin().get<LoggingRepository>().logWarning("[NotificationManager] Failed to parse notification payload: ${e.message}")
            }
            null
        }
    }

    private fun PayloadData.toNotificationResponse(): NetworkUtils.NotificationResponse? {
        val data = this
        
        return try {
            // Use kotlinx.serialization to deserialize from map
            val jsonElement = JsonObject(
                data.mapValues { (_, value) ->
                    when (value) {
                        is String -> JsonPrimitive(value)
                        is Boolean -> JsonPrimitive(value)
                        is Number -> JsonPrimitive(value)
                        null -> JsonNull
                        else -> JsonPrimitive(value.toString())
                    }
                }
            )
            
            val json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "type"
            }
            
            json.decodeFromJsonElement<NetworkUtils.NotificationResponse>(jsonElement)
        } catch (e: Exception) {
            runBlocking {
                KoinPlatform.getKoin().get<LoggingRepository>().logWarning("[NotificationManager] Failed to deserialize notification: ${e.message}")

            }
            null
        }
    }

    private fun NetworkUtils.NotificationResponse.toNotificationObject(): NotificationObject {
        return when (this) {
            is NetworkUtils.NotificationResponse.MessageNotificationResponse -> {
                NotificationObject.MessageNotification(
                    msgId = this.msgId,
                    senderName = this.senderName,
                    groupMessage = this.groupMessage,
                    groupName = this.groupName,
                    encodedContent = this.encodedContent
                )
            }
            is NetworkUtils.NotificationResponse.FriendRequestNotificationResponse -> {
                NotificationObject.FriendRequestNotification(
                    requesterId = this.requesterId,
                    requesterName = this.requesterName,
                )
            }
            is NetworkUtils.NotificationResponse.SystemNotificationResponse -> {
                NotificationObject.SystemNotification(
                    title = this.title,
                    message = this.message,
                )
            }
        }
    }



    private var initialized = false

    /**
     * Initialize the Notificationmanager (Listeners etc)
     * Should be called once during app startup
     */
    fun initialize() {
        if (initialized) {
            println("NotificationManager already initialized, skipping")
            return
        }

        try {
            NotifierManager.setLogger { message ->
                println("KMPNotifier log: $message")
            }

            // Token change listener
            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onNewToken(token: String) {

                }
            })

            // Payload data listener
            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onPayloadData(data: PayloadData) {
                    println("[NotificationManager] Push Notification received with payload: $data")

                    //Inject preferencemanager
                    val preferenceManager = KoinPlatform.getKoin().get<Preferencemanager>()



                    val notiThread = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            //get notiobject from payload data
                            val notiObject = data.toNotificationObject()

                            if (notiObject == null) {
                                println("[NotificationManager] ERROR: Failed to parse notification payload")
                                showNotification("Schneaggchat Error", "Failed to parse notification data")
                                return@launch
                            }

                            println("[NotificationManager] Parsed notification: $notiObject")

                            // Check if app is open before showing notification
                            if (AppLifecycleManager.isAppOpen()) {
                                println("[NotificationManager] App is open, skipping notification display")
                                return@launch
                            }

                            // Handle different notification types
                            when (notiObject) {
                                is NotificationObject.MessageNotification -> {
                                    // Load encryption key for message decryption
                                    val encryptionkey = withContext(Dispatchers.IO) {
                                        preferenceManager.getEncryptionKey()
                                    }

                                    if (encryptionkey.isEmpty()) {
                                        println("[NotificationManager] WARNING: Encryption key is empty")
                                        showNotification("Schneaggchat", "New message received (encryption key not available)")
                                        return@launch
                                    }

                                    // Build title string for message notifications
                                    val finaltitlestr = if (notiObject.groupMessage) {
                                        getString(Res.string.new_message_noti_group_title, notiObject.senderName, notiObject.groupName)
                                    } else {
                                        getString(Res.string.new_message_noti_single_title, notiObject.senderName)
                                    }

                                    // Try to decrypt and show notification
                                    try {
                                        // Decrypt on IO thread
                                        val decryptedContent = withContext(Dispatchers.IO) {
                                            notiObject.getDecodedContent(encryptionkey)
                                        }

                                        // Show notification
                                        showNotification(
                                            titletext = finaltitlestr,
                                            bodytext = decryptedContent
                                        )

                                        //Start datasync (May get cancelled but we dont care)
                                        val appRepository = KoinPlatform.getKoin().get<AppRepository>()
                                        appRepository.dataSync()
                                    } catch (e: Exception) {
                                        KoinPlatform.getKoin().get<LoggingRepository>().logWarning("[NotificationManager] Decryption failed: ${e.message}")

                                        // Show error notification
                                        showNotification(
                                            titletext = "$finaltitlestr (Decryption Error)",
                                            bodytext = "Failed to decrypt message: ${e.message}"
                                        )
                                    }
                                }

                                is NotificationObject.FriendRequestNotification -> {
                                    // Handle friend request notification
                                    showNotification(
                                        titletext = getString(Res.string.new_friend_request_noti, notiObject.requesterName),
                                        bodytext = getString(Res.string.new_friend_request_noti_body, notiObject.requesterName)
                                    )
                                }

                                is NotificationObject.SystemNotification -> {
                                    // Handle system notification
                                    showNotification(
                                        titletext = notiObject.title,
                                        bodytext = notiObject.message
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            KoinPlatform.getKoin().get<LoggingRepository>().logError("[NotificationManager] Unexpected error in notification handler: ${e.message}")

                            // Show error notification
                            showNotification("Schneaggchat Error", "Notification error: ${e.message}")
                        }
                    }

                    // Wait for the notification process to complete to prevent process death
                    runBlocking {
                        notiThread.join()
                    }

                    // Sync data (Ignore if app is open or not etc)
                    val appRepository = KoinPlatform.getKoin().get<AppRepository>()
                    CoroutineScope(Dispatchers.IO).launch {
                        appRepository.dataSync()
                    }
                }
            })

            // Notification clicked listener
            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onNotificationClicked(data: PayloadData) {
                    super.onNotificationClicked(data)
                    println("Notification clicked with payload: $data")
                    // TODO: Handle navigation or other actions
                }
            })

            initialized = true
            println("NotificationManager initialization completed")

        } catch (e: Exception) {
            runBlocking {
                KoinPlatform.getKoin().get<LoggingRepository>().logError("Error initializing NotificationManager: ${e.message}")
            }
        }
    }



    /**
     * Show a basic notification
     */
    fun showNotification(titletext: String, bodytext: String) {
        val notifier = NotifierManager.getLocalNotifier()

        val notiidnn = Random.nextInt(0, Int.MAX_VALUE)


        notifier.notify {
            id= notiidnn
            title = titletext
            body = bodytext
        }
    }


    fun showNotification(message: Message) {
        // Check if app is open before showing notification
        if (AppLifecycleManager.isAppOpen()) {
            println("[NotificationManager] App is open, skipping message notification display")
            return
        }
        
        val senderstring = message.senderAsString
        val content = if (message.isPicture()) "Pic" else message.content

        showNotification(senderstring, content)
    }

    /**
     * Remove notification, if no id is passed then all notifications
     * @param notiid Id of notification to remove
     */
    fun removeNotification(notiid: Int? = null){
        val notifier = NotifierManager.getLocalNotifier()
        if (notiid == null){
            notifier.removeAll()
        }else {
            notifier.remove(notiid)
        }
    }


    suspend fun getToken() : String {
        return NotifierManager.getPushNotifier().getToken() ?: ""
    }

    suspend fun removeToken(){
        NotifierManager.getPushNotifier().deleteMyToken()
    }



}