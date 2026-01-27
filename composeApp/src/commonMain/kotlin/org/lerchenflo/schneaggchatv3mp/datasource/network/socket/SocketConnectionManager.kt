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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository

/**
 * Socket connection manager for real-time communication
 * Integrates with existing Ktor HTTP client infrastructure
 */
class SocketConnectionManager(
    private val httpClient: HttpClient,
    private val loggingRepository: LoggingRepository,
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) {

    companion object {
        fun getSocketUrl(url: String): String {
            val cleanUrl = url.trimEnd('/')

            val wsUrl = when {
                cleanUrl.startsWith("https://") -> cleanUrl.replaceFirst("https://", "wss://")
                cleanUrl.startsWith("http://") -> cleanUrl.replaceFirst("http://", "ws://")
                cleanUrl.startsWith("wss://") || cleanUrl.startsWith("ws://") -> cleanUrl
                else -> {
                    println("Invalid URL format: $url. Expected http:// or https://")
                    "wss://$cleanUrl"
                }
            }

            val finalUrl = if (wsUrl.endsWith("/ws")) {
                wsUrl
            } else {
                "$wsUrl/ws"
            }

            println("WebSocket URL: $finalUrl")
            return finalUrl
        }
    }

    private var currentConnection: SocketConnection? = null
    private val _connectionState = MutableStateFlow(false)

    /**
     * Observable connection state for Composables
     */
    val isConnected: StateFlow<Boolean> = _connectionState.asStateFlow()

    /**
     * Establish WebSocket connection with authentication
     */
    suspend fun connect(
        serverUrl: String,
        onError: (Throwable) -> Unit,
        onClose: () -> Unit
    ): Boolean {
        return try {
            // Close existing connection if the serverUrl changed
            if (currentConnection?.serverUrl != serverUrl) {
                currentConnection?.close()
                _connectionState.value = false
            }

            // Create WebSocket connection
            val connection = SocketConnection(
                httpClient = httpClient,
                serverUrl = serverUrl,
                onMessage = {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleSocketConnectionMessage(it)
                    }
                },
                onError = onError,
                onClose = {
                    _connectionState.value = false
                    onClose()
                },
                onConnectionStateChanged = { isActive ->
                    _connectionState.value = isActive
                }
            )

            currentConnection = connection

            // Launch connection in background
            CoroutineScope(Dispatchers.IO).launch {
                connection.connect()
            }

            true // Connection attempt initiated successfully
        } catch (e: Exception) {
            loggingRepository.logError("Failed to connect WebSocket: ${e.message}")
            _connectionState.value = false
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

    /**
     * Close current connection
     */
    suspend fun close() {
        println("Closing socket connection")
        currentConnection?.close()
        currentConnection = null
        _connectionState.value = false
    }

    /**
     * Check if connection is active (synchronous check)
     */
    fun isConnectedNow(): Boolean = _connectionState.value

    // Add this method to handle incoming messages
    private suspend fun handleSocketConnectionMessage(message: String) {
        // Your message handling logic here
        println("Received message: $message")
    }
}

/**
 * Individual WebSocket connection handler
 */
private class SocketConnection(
    private val httpClient: HttpClient,
    val serverUrl: String,
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onClose: () -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit
) {
    private var session: WebSocketSession? = null
    private val _isActive = MutableStateFlow(false)

    suspend fun connect() {
        try {
            httpClient.webSocket(
                request = {
                    url(serverUrl)
                }
            ) {
                session = this
                _isActive.value = true
                onConnectionStateChanged(true)

                try {
                    incoming.receiveAsFlow()
                        .filterIsInstance<Frame.Text>()
                        .map { it.readText() }
                        .collect { message ->
                            onMessage(message)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError(e)
                } finally {
                    _isActive.value = false
                    onConnectionStateChanged(false)
                    session = null
                    onClose()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isActive.value = false
            onConnectionStateChanged(false)
            onError(e)
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
            onConnectionStateChanged(false)
            session = null
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore close errors
        }
    }

    fun isActive(): Boolean = _isActive.value
}