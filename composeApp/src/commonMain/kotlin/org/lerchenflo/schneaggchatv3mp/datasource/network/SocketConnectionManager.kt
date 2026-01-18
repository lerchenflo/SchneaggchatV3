package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository

/**
 * Socket connection manager for real-time communication
 * Integrates with existing Ktor HTTP client infrastructure
 */
class SocketConnectionManager(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val loggingRepository: LoggingRepository
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
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit,
        onClose: () -> Unit
    ): Boolean {
        return try {
            // Close existing connection if th e serverurl changed
            if (currentConnection?.serverUrl != serverUrl) {
                currentConnection?.close()
            }
            
            // Get auth token
            val tokens = tokenManager.loadBearerTokens()
            val authToken = tokens?.accessToken ?: return false
            
            // Create WebSocket connection
            val connection = SocketConnection(
                httpClient = httpClient,
                serverUrl = serverUrl,
                authToken = authToken,
                onMessage = onMessage,
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
    
    /**
     * Close current connection
     */
    suspend fun close() {
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
    private val authToken: String,
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onClose: () -> Unit
) {
    private var session: WebSocketSession? = null
    private var isActive = false
    
    suspend fun connect(): Boolean {
        return try {
            httpClient.webSocket(
                request = {
                    url(serverUrl)

                    /*
                    headers {
                        append("Authorization", "Bearer $authToken")
                    }

                     */
                }
            ) {
                session = this
                isActive = true
                
                try {
                    // Listen for incoming messages
                    incoming.receiveAsFlow()
                        .filterIsInstance<Frame.Text>()
                        .map { it.readText() }
                        .collect { message ->
                            onMessage(message)
                        }
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    isActive = false
                    onClose()
                }
            }
            true
        } catch (e: Exception) {
            onError(e)
            false
        }
    }
    
    suspend fun send(message: String): Boolean {
        return try {
            session?.send(Frame.Text(message))
            true
        } catch (e: Exception) {
            onError(e)
            false
        }
    }
    
    suspend fun close() {
        try {
            session?.close()
            isActive = false
        } catch (e: Exception) {
            // Ignore close errors
        }
    }
    
    fun isActive(): Boolean = isActive
}