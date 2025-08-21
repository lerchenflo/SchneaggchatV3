package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.compose.getKoin
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.SESSIONID
import org.lerchenflo.schneaggchatv3mp.chat.domain.DeleteUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllMessagesForUserIdUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllMessagesWithReadersUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDao
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

class SharedViewModel(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase,
    private val getChangeIdUserUseCase: GetChangeIdUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getChangeIdMessageUseCase: GetChangeIdMessageUseCase,
    private val upsertMessageUseCase: UpsertMessageUseCase,
    private val getAllMessagesWithReadersUseCase: GetAllMessagesWithReadersUseCase,
    private val getAllMessagesForUserIdUseCase: GetAllMessagesForUserIdUseCase,

    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager

):ViewModel() {

    init {
        print("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")

        //executeuserandmsgidsync()
    }


    fun executeuserandmsgidsync(onLoadingStateChange: (Boolean) -> Unit){
        viewModelScope.launch {
            supervisorScope { //Es kann uana crashen aber da andre ned
                delay(1000)

                networkUtils.executeUserIDSync(
                    getChangeIdUserUseCase = getChangeIdUserUseCase,
                    deleteUserUseCase = deleteUserUseCase,
                    upsertUserUseCase = upsertUserUseCase,
                    networkUtils = networkUtils
                )

                networkUtils.executeMsgIDSync(
                    getChangeIdMessageUseCase = getChangeIdMessageUseCase,
                    upsertMessageUseCase = upsertMessageUseCase,
                    networkUtils = networkUtils,
                    onLoadingStateChange = {onLoadingStateChange(it)},
                )
            }

        }
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



    // user getten und inserten
    val upsertUser = fun(user: User){
        viewModelScope.launch { upsertUserUseCase(user) }
    }

    fun getAllUsers(searchterm: String = ""): Flow<List<User>> {
        return getAllUserUseCase(searchterm)
    }


    fun getusersWithLastMessage(searchterm: String): Flow<List<User>> {
        val usersFlow = getAllUserUseCase(searchterm)
        val messagesFlow = getAllMessagesWithReadersUseCase()

        return combine(usersFlow, messagesFlow) { users, messagesWithReaders ->
            val messages = messagesWithReaders.map { it.message }

            users.map { user ->
                val last = messages
                    .filter { it.sender == user.id || it.receiver == user.id }
                    .maxByOrNull { it.getSendDateAsLong() }

                // return a copy of the user with lastmessage assigned
                user.apply { lastmessage = last }
            }
        }
    }

    fun getMessagesForUserId(userid: Long): Flow<List<MessageWithReaders>> {
        return getAllMessagesForUserIdUseCase(userid)
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