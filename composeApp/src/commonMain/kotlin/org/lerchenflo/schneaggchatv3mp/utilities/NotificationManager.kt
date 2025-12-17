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
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_group_title
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_single_title
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import kotlin.random.Random

object NotificationManager{


    data class NotificationObject(
        val msgId: String,
        val senderName: String,
        val groupMessage: Boolean,
        val groupName : String,
        val encodedContent: String
    ){
        suspend fun getDecodedContent(key: String) : String {
            return CryptoUtil.decrypt(encodedContent, key)
        }
    }

    private fun PayloadData.toNotificationObject(): NotificationObject? {
        val data = this

        try {
            val msgId = data["msgId"]?.toString() ?: ""
            val senderName = data["senderName"]?.toString() ?: "Unknown"
            val encodedContent = data["encodedContent"]?.toString() ?: ""
            val groupMessage = data["groupMessage"]?.toString()?.toBoolean() ?: false
            val groupName = data["groupName"]?.toString() ?: ""

            // Validate required fields
            if (msgId.isEmpty() || encodedContent.isEmpty()) {
                println("[NotificationManager] ERROR: Missing required fields in payload. msgId='$msgId', encodedContent isEmpty=${encodedContent.isEmpty()}")
                return null
            }

            return NotificationObject(
                msgId = msgId,
                senderName = senderName,
                encodedContent = encodedContent,
                groupMessage = groupMessage,
                groupName = groupName
            )
        } catch (e: Exception) {
            println("[NotificationManager] ERROR: Failed to parse notification payload: ${e.message}")
            e.printStackTrace()
            return null
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
                    println("onNewToken: $token")
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
                            //Load encryptionkey (IO operation)
                            val encryptionkey = withContext(Dispatchers.IO) {
                                preferenceManager.getEncryptionKey()
                            }

                            if (encryptionkey.isEmpty()) {
                                println("[NotificationManager] WARNING: Encryption key is empty")
                                showNotification("Schneaggchat", "New message received (encryption key not available)")
                                return@launch
                            }

                            //get notiobject from payload data
                            val notiObject = data.toNotificationObject()

                            if (notiObject == null) {
                                println("[NotificationManager] ERROR: Failed to parse notification payload")
                                showNotification("Schneaggchat Error", "Failed to parse notification data")
                                return@launch
                            }

                            println("[NotificationManager] Parsed notification: $notiObject")

                            // Build title string
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

                                // Show notification (already on Main thread)
                                showNotification(
                                    titletext = finaltitlestr,
                                    bodytext = decryptedContent
                                )
                            } catch (e: Exception) {
                                println("[NotificationManager] ERROR: Decryption failed: ${e.message}")
                                e.printStackTrace()

                                // Show error notification
                                showNotification(
                                    titletext = "$finaltitlestr (Decryption Error)",
                                    bodytext = "Failed to decrypt message: ${e.message}"
                                )
                            }
                        } catch (e: Exception) {
                            println("[NotificationManager] ERROR: Unexpected error in notification handler: ${e.message}")
                            e.printStackTrace()

                            // Show error notification
                            showNotification("Schneaggchat Error", "Notification error: ${e.message}")
                        }
                    }

                    // Wait for the notification process to complete to prevent process death
                    runBlocking {
                        notiThread.join()
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
            println("Error initializing NotificationManager: ${e.message}")
            e.printStackTrace()
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