package org.lerchenflo.schneaggchatv3mp.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.LOGGEDIN
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.SESSIONID
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.tables.User
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

class SharedViewModel(

    private val appRepository: AppRepository,
    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager

):ViewModel() {

    init {
        print("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")

        //executeuserandmsgidsync()

        //TODO: Sync alle 10 sek oda so der an login macht wenn du offline bisch und location und so
    }


    fun executeuserandmsgidsync(onLoadingStateChange: (Boolean) -> Unit){
        viewModelScope.launch {
            appRepository.executeSync(onLoadingStateChange)
        }
    }

    suspend fun areLoginCredentialsSaved(): Boolean{
        val (username, password) = preferencemanager.getAutologinCreds()
        if (username.isNotBlank() && password.isNotBlank()){
            OWNID = preferencemanager.getOWNID()
        }
        viewModelScope.launch {
            login(username, password, onResult = { success, body ->
                LOGGEDIN = success
                println("LOGGEDIN $success")
            })
        }
        return username.isNotBlank() && password.isNotBlank()
    }

    fun login(
        username: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            networkUtils.login(username, password)
                .onSuccessWithBody { headers, message ->
                    //println("Success: $success $message")
                    viewModelScope.launch {
                        preferencemanager.saveAutologinCreds(username, password)
                        preferencemanager.saveOWNID(headers["userid"]?.toLong() ?: 0)
                    }

                    println(headers)
                    SESSIONID = headers["sessionid"]
                    OWNID = headers["userid"]?.toLong()
                    println("SESSIONID gesetzt: $SESSIONID")
                    onResult(true, message)
                }
                .onError { error ->
                    println("Error: $error")

                    onResult(false, error.toString())
                }
        }
    }

    fun createAccount(
        username: String,
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            networkUtils.createAccount(username, password, email)
                .onSuccessWithBody { success, message ->
                    println("Success: $success $message")
                    onResult(success, message)
                }
                .onError { error ->
                    println("Error: $error")

                    onResult(false, error.toString())
                }
        }
    }



    fun getusersWithLastMessage(searchterm: String): Flow<List<User>> {
        val usersFlow = appRepository.getallusers(searchterm)
        val messagesFlow = appRepository.getAllMessagesWithReaders()

        return combine(usersFlow, messagesFlow) { users, messagesWithReaders ->

            users.map { user ->
                val last = messagesWithReaders
                    .filter { it.message.sender == user.id || it.message.receiver == user.id }
                    .maxByOrNull { it.getSendDateAsLong() }

                // return a copy of the user with lastmessage assigned
                user.apply { lastmessage = last }
            }
        }
    }

    fun getMessagesForUserId(userid: Long): Flow<List<MessageWithReaders>> {
        return appRepository.getMessagesByUserId(userid)
    }





    //aktuell ausgewählter chat
    private val _selectedChat = MutableStateFlow<User?>(null)
    val selectedChat = _selectedChat.asStateFlow()

    fun onSelectChat(user: User?) {
        _selectedChat.value = user
    }

    fun onLeaveChat(){
        _selectedChat.value = null
    }
}