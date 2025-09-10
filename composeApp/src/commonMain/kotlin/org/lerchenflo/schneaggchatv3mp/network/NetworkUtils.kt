package org.lerchenflo.schneaggchatv3mp.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.ServerGroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.ServerMessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.convertServerGroupDtoToGroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.convertServerMessageDtoToMessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.database.IdOperation
import org.lerchenflo.schneaggchatv3mp.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoEntityDto
import org.lerchenflo.schneaggchatv3mp.todolist.data.toTodoEntityDto
import org.lerchenflo.schneaggchatv3mp.todolist.data.toTodoEntry
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import org.lerchenflo.schneaggchatv3mp.utilities.Base64Util
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient
) {
    private val SERVERURL = "https://schneaggchatv3.lerchenflo.eu"

    /**
     * Perform a network request (GET when `get == true`, POST otherwise).
     *
     * - Encodes outgoing header values with Base64.
     * - Decodes incoming header values from Base64 (if decode fails, returns the raw header string).
     *
     * Returns Result.Success(map) where map contains all decoded response headers (lowercased keys)
     * and the response body under the key "body". On error returns Result.Error(NetworkError.*).
     */
    suspend fun <T> executeNetworkOperation(
        headers: Map<String, String>? = null,
        body: T? = null,
        get: Boolean = true,
        requestTimeoutMillis: Long = 20_000L
    ): NetworkResult<Map<String, String>, String> {
        try {
            val response: HttpResponse = httpClient.request {
                url(SERVERURL)
                method = if (get) HttpMethod.Get else HttpMethod.Post

                // Per-request timeout (requires HttpTimeout plugin to be installed in client).
                timeout {
                    this.requestTimeoutMillis = requestTimeoutMillis
                }

                // Built-in header: handy time (milliseconds)
                headers {
                    append("handytime", Base64Util.encode(Clock.System.now().toEpochMilliseconds().toString()))

                    SessionCache.getOwnIdValue()?.let {
                        append("sessionid", Base64Util.encode(it.toString()))
                    }

                    // Additional headers (encode values)
                    headers?.forEach { (k, v) ->
                        // normalize keys: trim newlines, toLowerCase
                        val key = k.replace("\n", "").lowercase()
                        val value = v.replace("\n", "")
                        if (key.isNotBlank() && value.isNotBlank()) {
                            header(key, Base64Util.encode(value))
                        }
                    }
                }

                // Body for POST
                if (!get && body.toString() != "") {
                    contentType(ContentType.Application.Json)
                    if (body != null){
                        setBody(body)
                    }
                }
            }

            val responseBody = response.bodyAsText()

            // Decode response headers
            val decodedHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { rawName ->
                // normalize key: strip newlines + lowercase
                val key = rawName.replace("\n", "").lowercase()
                val joined = response.headers.getAll(rawName)?.joinToString(",") ?: ""
                val decodedValue = try {
                    Base64Util.decode(joined)
                } catch (e: IllegalArgumentException) {
                    // not valid Base64, keep raw
                    joined
                }
                decodedHeaders[key] = decodedValue
            }


            //Hot die operation gfunkt
            if (!decodedHeaders["successful"].toBoolean()){
                return NetworkResult.Error(responseBody)
            }


            return NetworkResult.Success( decodedHeaders,responseBody.toString())

        } catch (e: UnresolvedAddressException) {
            return NetworkResult.Error(ResponseReason.NO_INTERNET.toString())
        } catch (e: HttpRequestTimeoutException) {
            return NetworkResult.Error(ResponseReason.TIMEOUT.toString())
        } catch (e: SocketTimeoutException) {
            return NetworkResult.Error(ResponseReason.TIMEOUT.toString())
        } catch (e: SocketTimeoutException) {
            return NetworkResult.Error(ResponseReason.TIMEOUT.toString())
        } catch (e: Exception) {
            // You can log e.message here if you have a logger
            return NetworkResult.Error(ResponseReason.unknown_error.toString())
        }
    }



    suspend fun login(username: String, password: String): NetworkResult<Map<String, String>, String> {
        val headers = mapOf(
            "msgtype" to LOGINMESSAGE,
            "username" to username,
            "password" to password
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success( res.data, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun createAccount(username: String, password: String, email: String, gender: String, birthdate: String): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to CREATEACCOUNTMESSAGE,
            "username" to username,
            "password" to password,
            "email" to email,
            "gender" to gender,
            "birthdate" to birthdate
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }


    suspend fun useridsync(databaseids: String): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to USERIDSYNC,
        )

        val res = executeNetworkOperation(headers = headers, body = databaseids, get = false)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun getuserbyid(id: Long): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to GETUSERBYID,
            "userid" to id.toString()
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = true)
        //TODO: User returnen
        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }


    suspend fun groupidsync(databaseids: String): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to GROUPIDSYNC,
        )

        val res = executeNetworkOperation(headers = headers, body = databaseids, get = false)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun getgroupbyid(id: Long): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to GETGROUPBYID,
            "groupid" to id.toString()
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        //Todo: Group objekt returnen

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }




    suspend fun todoidsync(databaseids: String): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to BUGFEATURESYNC,
        )

        val res = executeNetworkOperation(headers = headers, body = databaseids, get = false)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun gettodobyid(id: Long): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to GETBUGFEATUREBYID,
            "bugfeatureid" to id.toString()
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun upsertTodo(todo: TodoEntry): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to ADDBUGFEATURE
        )

        val res = executeNetworkOperation(headers = headers, body = todo.toTodoEntityDto() , get = false)
        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    suspend fun deleteTodo(todoId: Long): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to DELETEBUGFEATUREBYID,
            "bugfeatureid" to todoId.toString()
        )

        val res = executeNetworkOperation(headers = headers, body = "", get = false)
        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }





    suspend fun messageidsync(databaseids: String): NetworkResult<Map<String, String>, String> {
        val headers = mapOf(
            "msgtype" to GETMESSAGESWITHOUTPICTURES,
        )

        val res = executeNetworkOperation(headers = headers, body = databaseids, get = false)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(res.data, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

    //Theoretisch nur no für bilder
    suspend fun getmessagebyid(id: Long): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to GETMESSAGEBYID,
            "msgid" to id.toString()
        )

        //TODO: A message object returnen

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }


    //Do isch da netzwerkteil vom message senden (gmergta teil im appRepositor)
    suspend fun sendMessageToServer(msgtype: String, empfaenger: Long, gruppe: Boolean, message: String, answerid: Long, sendedatum: String): NetworkResult<Map<String, String>, String> {
        val headers = mapOf(
            "msgtype" to msgtype,
            "empfaenger" to empfaenger.toString(),
            "sendedatum" to sendedatum,
            "answerid" to answerid.toString(),
            "groupmessage" to gruppe.toString()
        )

        return executeNetworkOperation(headers = headers, body = message, get = false)
    }





    suspend fun executeUserIDSync(userRepository: UserRepository) {

        println("Useridsync STARTET")

        try {

            val json = Json {
                prettyPrint = false
                ignoreUnknownKeys = true
            }

            // 1. Get local user IDs and change dates
            val localUsers = userRepository.getuserchangeid()
            val serializedData = json.encodeToString(localUsers)

            // 2. Execute user ID sync with server
            val syncResult = useridsync(serializedData)

            syncResult.onSuccessWithBody { success, body ->
                val operations = json.decodeFromString<List<IdOperation>>(body)
                CoroutineScope(Dispatchers.Default).launch {
                    operations.map { operation ->
                        when (operation.Status) {
                            "deleted" -> {
                                try {

                                    userRepository.deleteUser(operation.Id)
                                    Result.success(Unit)
                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                            "new", "modified" -> {
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val userResult = getuserbyid(operation.Id)
                                        userResult.onSuccessWithBody { success, body ->

                                            //println(body)
                                            val user = json.decodeFromString<User>(body)

                                            CoroutineScope(Dispatchers.IO).launch {
                                                userRepository.upsertUser(user)
                                                println("User inserted: ${user.name}")
                                            }
                                        }
                                    }

                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                        }
                    }
                }

            }
            syncResult.onError {
                println(it)
            }
        } catch (e: Exception) {
            // Log error (platform-specific logging would be implemented separately)
            println("Useridsync fail")
            e.printStackTrace()
        }

    }


    suspend fun executeGroupIDSync(groupRepository: GroupRepository) {

        println("Groupidsync STARTET")


        try {

            val json = Json {
                prettyPrint = false
                //ignoreUnknownKeys = true
            }

            // 1. Get local user IDs and change dates
            val localGroups = groupRepository.getgroupchangeid()
            val serializedData = json.encodeToString(localGroups)

            // 2. Execute user ID sync with server
            val syncResult = groupidsync(serializedData)

            syncResult.onSuccessWithBody { success, body ->
                val operations = json.decodeFromString<List<IdOperation>>(body)
                CoroutineScope(Dispatchers.Default).launch {
                    operations.map { operation ->
                        when (operation.Status) {
                            "deleted" -> {
                                try {
                                    groupRepository.deleteGroup(operation.Id)
                                    Result.success(Unit)
                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                            "new", "modified" -> {
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val groupResult = getgroupbyid(operation.Id)
                                        groupResult.onSuccessWithBody { success, body ->

                                            //Einzelne gruppe isch ako, jetzt in a liste vo dtos verwandla
                                            val serverlist = json.decodeFromString<ServerGroupDto>(body)

                                            val group = convertServerGroupDtoToGroupWithMembers(serverlist)

                                            CoroutineScope(Dispatchers.IO).launch {
                                                groupRepository.upsertGroupWithMembers(group)
                                                println("Group inserted: ${group}")
                                            }
                                        }
                                    }

                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                        }
                    }
                }

            }
            syncResult.onError {
                println(it)
            }
        } catch (e: Exception) {
            // Log error (platform-specific logging would be implemented separately)
            println("Groupidsync fail")
            e.printStackTrace()
        }

    }




    suspend fun executeMsgIDSync(messageRepository: MessageRepository, onLoadingStateChange: (Boolean) -> Unit) {

        //TODO: Richta dassas ersch startet wenn da vorherige request fertig isch
        println("MSgidsync startet")
        onLoadingStateChange(true)

        var moremessages = true
        var errorcount = 0

        try {

            val json = Json {
                prettyPrint = false
                //ignoreUnknownKeys = true
            }

            //println("Json messagewithreader ${json.encodeToString(ServerMessageDto)}")

            // 1. Get local user IDs and change dates
            while (moremessages){

                val localMessages = messageRepository.getmessagechangeid()
                val serializedData = json.encodeToString(localMessages)

                println("Msgidsync: $serializedData")

                val syncResult = messageidsync(serializedData)

                syncResult.onSuccessWithBody { responseheaders, body ->

                    if (moremessages){
                        moremessages = responseheaders["moremessages"].toBoolean()
                    }
                    println("Messageidsync: es git mehr messages zum hola: $moremessages")

                    val serverlist = json.decodeFromString<List<ServerMessageDto>>(body)

                    if (serverlist.size > 1){
                        println("Startid: ${serverlist[0].id}")
                    }

                    val messageListwithReaders = convertServerMessageDtoToMessageWithReaders(serverlist)

                    val (normalMessages, pictureMessages) = messageListwithReaders.partition { !it.message.isPicture() }

                    if (normalMessages.isNotEmpty()) {

                        //Starta und warta bis fertig inkjected
                        withContext(Dispatchers.IO) {
                            messageRepository.upsertMessagesWithReaders(normalMessages)
                        }
                    }


                    //Bilder einzeln hola
                    for (m in pictureMessages){
                        try {
                            val id = m.message.id
                            val messageResult = id?.let { getmessagebyid(it) }

                            if (messageResult == null) {
                                println("ERROR: Messageid nicht vorhanden")
                            } else {
                                try {
                                    var messageToUpsert: MessageWithReaders? = null

                                    // onSuccessWithBody is suspending — await it and convert the body
                                    messageResult.onSuccessWithBody { _, body1 ->
                                        val messageasdto = json.decodeFromString<ServerMessageDto>(body1)
                                        messageToUpsert = convertServerMessageDtoToMessageWithReaders(messageasdto)
                                    }

                                    // if successful, upsert and wait for DB write to finish before next iteration
                                    messageToUpsert?.let { msg ->
                                        withContext(Dispatchers.IO) {
                                            messageRepository.upsertMessageWithReaders(msg)
                                        }
                                        println("Picture message upserted: $id")
                                    } ?: println("Failed to fetch/parse picture message id $id")

                                } catch (e: Exception) {
                                    println("Error fetching picture message id $id: ${e.message}")
                                }
                            }


                        } catch (e: Exception) {
                            Result.failure<Unit>(e)
                        }

                    }


                }
                syncResult.onError {
                    println("Msgidsync error: $it")
                    errorcount ++

                    if (errorcount > 5){
                        moremessages = false
                    }
                }

                //delay bevor da nächste sync startet
                delay(1500)
            }
        } catch (e: Exception) {
            // Log error (platform-specific logging would be implemented separately)
            println("Useridsync fail")
            e.printStackTrace()
        }finally {
            onLoadingStateChange(false) // End loading
        }
    }


    suspend fun executeTodoIDSync(todoRepository: TodoRepository) {

        println("Todoidsync STARTET")


        try {

            val json = Json {
                prettyPrint = false
                //ignoreUnknownKeys = true
            }

            // 1. Get local user IDs and change dates
            val localTodos = todoRepository.gettodochangeid()
            val serializedData = json.encodeToString(localTodos)

            // 2. Execute user ID sync with server
            val syncResult = todoidsync(serializedData)

            syncResult.onSuccessWithBody { success, body ->
                val operations = json.decodeFromString<List<IdOperation>>(body)
                CoroutineScope(Dispatchers.Default).launch {
                    operations.map { operation ->
                        when (operation.Status) {
                            "deleted" -> {
                                try {
                                    todoRepository.deleteTodo(operation.Id)
                                    Result.success(Unit)
                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                            "new", "modified" -> {
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val todoResult = gettodobyid(operation.Id)
                                        todoResult.onSuccessWithBody { success, body ->

                                            val todo = json.decodeFromString<TodoEntityDto>(body)

                                            CoroutineScope(Dispatchers.IO).launch {
                                                todoRepository.upsertTodo(todo.toTodoEntry())
                                                println("Todo inserted: ${todo}")
                                            }
                                        }
                                    }

                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                        }
                    }
                }

            }
            syncResult.onError {
                println(it)
            }
        } catch (e: Exception) {
            // Log error (platform-specific logging would be implemented separately)
            println("Groupidsync fail")
            e.printStackTrace()
        }

    }



}

