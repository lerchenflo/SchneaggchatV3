package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import kotlin.math.min

/**
 * Socket connection manager for real-time communication
 * Integrates with existing Ktor HTTP client infrastructure
 */
class SocketConnectionManager(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager
) {

    enum class ConnectionState {
        Disconnected,
        Connecting,
        Connected
    }

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

            return finalUrl
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val connectMutex = Mutex()
    private var currentConnection: SocketConnection? = null

    private var connectJob: Job? = null
    private var reconnectJob: Job? = null

    private var lastServerUrl: String? = null
    private var lastOnError: ((Throwable) -> Unit)? = null
    private var lastOnClose: (() -> Unit)? = null

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)

    /**
     * Observable connection state for Composables
     */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: StateFlow<Boolean> = _connectionState
        .map { it == ConnectionState.Connected }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    /**
     * Establish WebSocket connection with authentication
     */
    suspend fun connect(
        serverUrl: String,
        onError: (Throwable) -> Unit,
        onClose: () -> Unit
    ): Boolean {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return false

        lastServerUrl = serverUrl
        lastOnError = onError
        lastOnClose = onClose

        return connectMutex.withLock {
            if (_connectionState.value == ConnectionState.Connected || _connectionState.value == ConnectionState.Connecting) {
                return@withLock true
            }

            _connectionState.value = ConnectionState.Connecting

            try {
                reconnectJob?.cancel()
                reconnectJob = null

                currentConnection?.close()
                currentConnection = null

                val connection = SocketConnection(
                    httpClient = httpClient,
                    serverUrl = serverUrl,
                    tokenManager = tokenManager,
                    onMessage = {
                        scope.launch {
                            handleSocketConnectionMessage(
                                ownId = ownId,
                                message = it
                            )
                        }
                    },
                    onError = { throwable ->
                        _connectionState.value = ConnectionState.Disconnected
                        onError(throwable)
                        scheduleReconnectIfPossible()
                    },
                    onClose = {
                        _connectionState.value = ConnectionState.Disconnected
                        onClose()
                        scheduleReconnectIfPossible()
                    },
                    onConnectionStateChanged = { isActive ->
                        _connectionState.value = if (isActive) ConnectionState.Connected else ConnectionState.Disconnected
                    }
                )

                currentConnection = connection

                connectJob?.cancel()
                connectJob = scope.launch {
                    try {
                        connection.connect()
                    } catch (t: Throwable) {
                        _connectionState.value = ConnectionState.Disconnected
                        onError(t)
                        scheduleReconnectIfPossible()
                    }
                }

                true
            } catch (e: Exception) {
                //loggingRepository.logError("Failed to connect WebSocket: ${e.message}")
                _connectionState.value = ConnectionState.Disconnected
                onError(e)
                scheduleReconnectIfPossible()
                false
            }
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
        reconnectJob?.cancel()
        reconnectJob = null
        connectJob?.cancel()
        connectJob = null

        currentConnection?.close()
        currentConnection = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Check if connection is active (synchronous check)
     */
    fun isConnectedNow(): Boolean = _connectionState.value == ConnectionState.Connected

    private fun scheduleReconnectIfPossible() {
        val url = lastServerUrl ?: return
        val onError = lastOnError ?: return
        val onClose = lastOnClose ?: return

        if (reconnectJob?.isActive == true) return
        if (!AppLifecycleManager.isAppInForeground) return

        reconnectJob = scope.launch {
            var attempt = 0
            while (_connectionState.value != ConnectionState.Connected) {
                val delayMs = min(30_000L, 1_000L * (1L shl min(attempt, 5)))
                attempt++
                delay(delayMs)
                if (!AppLifecycleManager.isAppInForeground) break
                connect(url, onError, onClose)
            }
        }
    }
}

/**
 * Individual WebSocket connection handler
 */
private class SocketConnection(
    private val httpClient: HttpClient,
    val serverUrl: String,
    private val tokenManager: TokenManager,
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onClose: () -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit
) {
    private var session: WebSocketSession? = null
    private val _isActive = MutableStateFlow(false)

    suspend fun connect() {
        val tokens = tokenManager.loadBearerTokens()
        try {
            connectWithToken(tokens?.accessToken)
        } catch (e: Exception) {
            val refreshError = tokenManager.refreshTokens(tokens?.refreshToken)
            if (refreshError == null) {
                val freshTokens = tokenManager.loadBearerTokens()
                try {
                    connectWithToken(freshTokens?.accessToken)
                } catch (retryEx: Exception) {
                    _isActive.value = false
                    onConnectionStateChanged(false)
                    onError(retryEx)
                }
            } else {
                _isActive.value = false
                onConnectionStateChanged(false)
                onError(e)
            }
        }
    }

    private suspend fun connectWithToken(accessToken: String?) {
        httpClient.webSocket(
            request = {
                url(serverUrl)
                accessToken?.let { bearerAuth(it) }
            }
        ) {
            session = this
            _isActive.value = true
            onConnectionStateChanged(true)
            //println("SocketConnection: connected to $serverUrl")

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
                onConnectionStateChanged(false)
                session = null
                //println("SocketConnection: disconnected from $serverUrl")
                onClose()
            }
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
            //e.printStackTrace()
            // Ignore close errors
        }
    }

    fun isActive(): Boolean = _isActive.value
}