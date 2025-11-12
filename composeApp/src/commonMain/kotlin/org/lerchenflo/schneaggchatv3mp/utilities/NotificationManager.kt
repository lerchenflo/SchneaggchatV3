package org.lerchenflo.schneaggchatv3mp.utilities

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import kotlin.random.Random

object NotificationManager{


    /**
     * Initialize the Notificationmanager (Listeners etc)
     */
    fun initialize(networkUtils: NetworkUtils) {

        try {
            NotifierManager.setLogger { message ->
                // Log the message
                println("KMPNotifier log: " + message)
            }

            //Token change listener
            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onNewToken(token: String) {
                    CoroutineScope(Dispatchers.IO).launch{
                        //TODO : Firebase
                        //networkUtils.setFirebaseToken(token)
                    }
                    println("onNewToken: $token") //Update user token in the server if needed
                }
            })


            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onPayloadData(data: PayloadData) {
                    println("Push Notification payloadData: $data") //PayloadData is just typeAlias for Map<String,*>.

                    val action = data.get("action")

                    when (action) {
                        "getmessagewithid" -> {
                            val msgid = data.get("msgid")

                            CoroutineScope(Dispatchers.IO).launch {
                                /* TODO FIREBASE
                                val request = networkUtils.getmessagebyid(msgid.toString().toLong())

                                val json = Json {
                                    prettyPrint = false
                                    ignoreUnknownKeys = true
                                }

                                request.onSuccessWithBody {responseheaders, body ->
                                    val message = json.decodeFromString<MessageDto>(body)
                                    showNotification(message.toMessage())
                                }

                                 */
                            }
                        }
                    }

                    showNotification("Neue noti", "Data: $data")
                }
            })


            NotifierManager.addListener(object : NotifierManager.Listener {
                override fun onNotificationClicked(data: PayloadData) {
                    super.onNotificationClicked(data)
                    println("Notification clicked, Notification payloadData: $data")
                }
            })

            CoroutineScope(Dispatchers.IO).launch {
                //TODO FIREBASE
                //networkUtils.setFirebaseToken(getToken())
            }
        }catch (e: Exception){
            e.printStackTrace()
        }



        println("Notificationmanager init fertig")
    }



    /**
     * Show a basic notification
     */
    fun showNotification(titletext: String, bodytext: String, notiId: Int? = null) {
        val notifier = NotifierManager.getLocalNotifier()

        val notiidnn = notiId ?: Random.nextInt(0, Int.MAX_VALUE)


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