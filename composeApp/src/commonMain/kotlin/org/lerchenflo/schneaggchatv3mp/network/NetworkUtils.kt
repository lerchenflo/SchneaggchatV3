package org.lerchenflo.schneaggchatv3mp.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.http.HttpMethod
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.SESSIONID
import org.lerchenflo.schneaggchatv3mp.chat.domain.DeleteUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.database.IdOperation
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.ServerMessageDto
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDao
import org.lerchenflo.schneaggchatv3mp.database.convertServerMessageDtoToMessageWithReaders
import org.lerchenflo.schneaggchatv3mp.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
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

                    SESSIONID?.let { append("sessionid", Base64Util.encode(it)) }
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

                // Body for POST
                if (!get && body.toString() != "") {
                    setBody(body.toString())
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

    suspend fun createAccount(username: String, password: String, email: String): NetworkResult<Boolean, String> {
        val headers = mapOf(
            "msgtype" to CREATEACCOUNTMESSAGE,
            "username" to username,
            "password" to password,
            "email" to email
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

        val res = executeNetworkOperation(headers = headers, body = "", get = true)

        return when (res) {

            is NetworkResult.Success -> {
                // 4. Access the body directly from the Success result
                NetworkResult.Success(true, res.body)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.error)
        }
    }

















    suspend fun executeUserIDSync(getChangeIdUserUseCase: GetChangeIdUserUseCase, deleteUserUseCase: DeleteUserUseCase, upsertUserUseCase: UpsertUserUseCase, networkUtils: NetworkUtils) {

        try {

            val json = Json {
                prettyPrint = false
                ignoreUnknownKeys = true
            }

            // 1. Get local user IDs and change dates
            val localUsers = getChangeIdUserUseCase()
            val serializedData = json.encodeToString(localUsers)

            // 2. Execute user ID sync with server
            val syncResult = networkUtils.useridsync(serializedData)

            syncResult.onSuccessWithBody { success, body ->
                val operations = json.decodeFromString<List<IdOperation>>(body)
                CoroutineScope(Dispatchers.Default).launch {
                    val results = operations.map { operation ->
                        when (operation.Status) {
                            "deleted" -> {
                                try {

                                    deleteUserUseCase(operation.Id)
                                    Result.success(Unit)
                                } catch (e: Exception) {
                                    Result.failure<Unit>(e)
                                }
                            }
                            "new", "modified" -> {
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val userResult = networkUtils.getuserbyid(operation.Id)
                                        userResult.onSuccessWithBody { success, body ->

                                            //println(body)
                                            val users = json.decodeFromString<List<User>>(body)

                                            CoroutineScope(Dispatchers.IO).launch {
                                                upsertUserUseCase(users[0])
                                                println("User inserted: ${users[0].id}")
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




    suspend fun executeMsgIDSync(getChangeIdMessageUseCase: GetChangeIdMessageUseCase, upsertMessageUseCase: UpsertMessageUseCase,networkUtils: NetworkUtils) {

        println("MSgidsync startet")

        var moremessages = true

        try {

            val json = Json {
                prettyPrint = false
                //ignoreUnknownKeys = true
            }

            //println("Json messagewithreader ${json.encodeToString(ServerMessageDto)}")

            // 1. Get local user IDs and change dates
            while (moremessages){
                val localMessages = getChangeIdMessageUseCase()
                val serializedData = json.encodeToString(localMessages)

                val syncResult = networkUtils.messageidsync(serializedData)

                syncResult.onSuccessWithBody { responseheaders, body ->

                    moremessages = responseheaders["moremessages"].toBoolean()
                    println("Messageidsync: es git $moremessages zum hola!")

                    val serverlist = json.decodeFromString<List<ServerMessageDto>>(body)

                    val messageListwithReaders = convertServerMessageDtoToMessageWithReaders(serverlist)

                    val (normalMessages, pictureMessages) = messageListwithReaders.partition { !it.message.isPicture() }

                    if (normalMessages.isNotEmpty()) {

                        //Non blocking Coroutine for Upsert message
                        CoroutineScope(
                            context = Dispatchers.IO
                        ).launch {
                            upsertMessageUseCase(normalMessages)
                            println("Message insert gmacht: ${normalMessages.size} messages")

                        }
                    }


                    /* TODO: Bilder hola
                    val deferreds = pictureMessages.map { m ->
                        async {
                            val messageResult = networkUtils.getmessagebyid(m.id)
                            messageResult.onSuccessWithBody { _, body1 ->
                                if (body1 != "[]") {
                                    val messages = json.decodeFromString<List<Message>>(body1)
                                    upsertMessageUseCase(messages[0]) // direct suspend call
                                }
                            }
                        }
                    }

                    deferreds.awaitAll()

                     */


                    //Bilder einzeln hola
                    for (m in pictureMessages){
                        try {
                            CoroutineScope(Dispatchers.IO).launch {
                                val messageResult = networkUtils.getmessagebyid(m.message.id)
                                messageResult.onSuccessWithBody { success, body1 ->

                                    val messageasdto = json.decodeFromString<List<ServerMessageDto>>(body1)

                                    val message = convertServerMessageDtoToMessageWithReaders(messageasdto)

                                    CoroutineScope(
                                        context = Dispatchers.IO
                                    ).launch {
                                        upsertMessageUseCase(message[0])
                                    }

                                }
                            }

                        } catch (e: Exception) {
                            Result.failure<Unit>(e)
                        }

                    }


                }
                syncResult.onError {
                    println("Msgidsync error: $it")
                }
            }
        } catch (e: Exception) {
            // Log error (platform-specific logging would be implemented separately)
            println("Useridsync fail")
            e.printStackTrace()
        }


    }



}

