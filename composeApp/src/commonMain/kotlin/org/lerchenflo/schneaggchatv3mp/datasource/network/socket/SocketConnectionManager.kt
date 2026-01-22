package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager

/**
 * Socket connection manager for real-time communication
 * Integrates with existing Ktor HTTP client infrastructure
 */
class SocketConnectionManager(
    private val httpClient: HttpClient,
    private val loggingRepository: LoggingRepository,
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository
) {

    companion object {
        fun getSocketUrl(url: String): String {
            val newurl = if (url.startsWith("https")) {
                url.replace("https", "wss")
            } else if (url.startsWith("http")){
                url.replace("http", "ws")
            } else {
                println("GetsocketURL error: $url")
                url
            }
            println("New socket url: $newurl/ws")
            return "$newurl/ws"
        }
    }



    private var currentConnection: SocketConnection? = null
    
    /**
     * Establish WebSocket connection with authentication
     */
    suspend fun connect(
        serverUrl: String,
        onError: (Throwable) -> Unit,
        onClose: () -> Unit
    ): Boolean {
        return try {
            // Close existing connection if th e serverurl changed
            if (currentConnection?.serverUrl != serverUrl) {
                currentConnection?.close()
            }

            // Create WebSocket connection
            val connection = SocketConnection(
                httpClient = httpClient,
                serverUrl = serverUrl,
                onMessage = {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleRemoteMessage(it)
                    }
                },
                onError = onError,
                onClose = onClose
            )
            
            if (connection.connect()) {
                currentConnection = connection
                loggingRepository.logInfo("WebSocket connected to $serverUrl")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            loggingRepository.logError("Failed to connect WebSocket: ${e.message}")
            onError(e)
            false
        }
    }
    
    /**
     * Send message through current connection
     */
    suspend fun sendMessage(message: String): Boolean {
        return currentConnection?.send(message) ?: false
    }

    suspend fun handleRemoteMessage(message: String) {
        //TODO flo deserialize message etc
//        when (message) {
//            messageRepository.upsertMessage()
//        }
    }
    
    /**
     * Close current connection
     */
    suspend fun close() {
        println("Closing socket connection")
        currentConnection?.close()
        currentConnection = null
    }
    
    /**
     * Check if connection is active
     */
    fun isConnected(): Boolean = currentConnection?.isActive() ?: false
}

/**
 * Individual WebSocket connection handler
 */
private class SocketConnection(
    private val httpClient: HttpClient,
    val serverUrl: String,
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onClose: () -> Unit
) {
    private var session: WebSocketSession? = null
    private val _isActive = MutableStateFlow(false)

    suspend fun connect(): Boolean {
        return try {
            CoroutineScope(Dispatchers.IO).launch {
                httpClient.webSocket(
                    request = {
                        url(serverUrl)
                    }
                ) {
                    session = this
                    _isActive.value = true

                    try {
                        incoming.receiveAsFlow()
                            .filterIsInstance<Frame.Text>()
                            .map { it.readText() }
                            .collect { message ->
                                onMessage(message)
                            }
                    } catch (e: Exception) {
                        onError(e)
                    } finally {
                        _isActive.value = false
                        session = null
                        onClose()
                    }
                }
            }

            // Wait for connection to establish
            delay(100)
            _isActive.value
        } catch (e: Exception) {
            onError(e)
            false
        }
    }

    suspend fun send(message: String): Boolean {
        return try {
            session?.send(Frame.Text(message)) ?: return false
            true
        } catch (e: Exception) {
            onError(e)
            false
        }
    }

    suspend fun close() {
        try {
            session?.close()
            _isActive.value = false
            session = null
        } catch (e: Exception) {
            // Ignore close errors
        }
    }

    fun isActive(): Boolean = _isActive.value
}