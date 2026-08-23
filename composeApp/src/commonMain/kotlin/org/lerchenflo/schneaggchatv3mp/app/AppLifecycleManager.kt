package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Manages app lifecycle state to determine if the app is in foreground/background
 */
object AppLifecycleManager {
    // A StateFlow (not Compose mutableStateOf) on purpose: this is read from plain coroutines
    // outside any Composable (e.g. GlobalViewModel's polling loop), and Compose snapshot state
    // read from outside the snapshot system can throw "Reading a state that was created after
    // the snapshot was taken".
    private val _isAppInForeground = MutableStateFlow(false)

    private val _appResumedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _appBackgroundedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val appResumedEvent: SharedFlow<Unit> = _appResumedEvent.asSharedFlow()
    val appBackgroundedEvent: SharedFlow<Unit> = _appBackgroundedEvent.asSharedFlow()

    /**
     * Whether the app is currently in foreground (visible to user)
     */
    val isAppInForeground: Boolean
        get() = _isAppInForeground.value

    /**
     * Update the app foreground state
     */
    internal fun updateAppForegroundState(isInForeground: Boolean) {
        _isAppInForeground.value = isInForeground
    }

    /**
     * Notify that the app has resumed
     */
    internal fun notifyAppResumed() {
        _appResumedEvent.tryEmit(Unit)
    }
    
    internal fun notifyAppBackgrounded() {
        _appBackgroundedEvent.tryEmit(Unit)
    }

    data class NotificationOpenData(
        val chatId: String? = null,
        val isGroup: Boolean = false
    )

    // A StateFlow (not a SharedFlow) on purpose: notification taps can arrive before any
    // collector exists yet (cold app start), and a SharedFlow with no subscribers just drops
    // the emission. Holding the latest value until it is explicitly consumed makes the request
    // durable across that startup race.
    private val _pendingNotificationOpen = MutableStateFlow<NotificationOpenData?>(null)
    val pendingNotificationOpen: StateFlow<NotificationOpenData?> = _pendingNotificationOpen.asStateFlow()

    fun notifyNotificationOpened(
        chatId: String? = null,
        isGroup: Boolean = false
    ) {
        _pendingNotificationOpen.value = NotificationOpenData(chatId = chatId, isGroup = isGroup)
    }

    /** Atomically reads and clears the pending request so it is only acted on once. */
    fun consumePendingNotificationOpen(): NotificationOpenData? {
        return _pendingNotificationOpen.getAndUpdate { null }
    }

    // Flips once the auth-gated startup navigation (auto-login / email-verify check) has run,
    // so a pending notification-open request is not wiped out by that navigation resetting the
    // backstack afterwards.
    private val _startupRoutingDone = MutableStateFlow(false)
    val startupRoutingDone: StateFlow<Boolean> = _startupRoutingDone.asStateFlow()

    fun notifyStartupRoutingDone() {
        _startupRoutingDone.value = true
    }

    /**
     * Check if app is open (in foreground) when receiving notifications
     * @return true if app is in foreground, false if app is in background
     */
    fun isAppOpen(): Boolean {
        return isAppInForeground
    }
}

/**
 * Composable that tracks app lifecycle and updates AppLifecycleManager
 * Should be called at the root of your app (e.g., in App.kt)
 */
@Composable
fun AppLifecycleTracker() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(Lifecycle.State.INITIALIZED) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    lifecycleState = Lifecycle.State.CREATED
                    AppLifecycleManager.updateAppForegroundState(false)
                }

                Lifecycle.Event.ON_START -> {
                    lifecycleState = Lifecycle.State.STARTED
                    AppLifecycleManager.updateAppForegroundState(true)
                }

                Lifecycle.Event.ON_RESUME -> {
                    lifecycleState = Lifecycle.State.RESUMED
                    AppLifecycleManager.updateAppForegroundState(true)
                    AppLifecycleManager.notifyAppResumed()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    lifecycleState = Lifecycle.State.CREATED
                    AppLifecycleManager.updateAppForegroundState(false)
                }

                Lifecycle.Event.ON_STOP -> {
                    lifecycleState = Lifecycle.State.CREATED
                    AppLifecycleManager.updateAppForegroundState(false)
                    AppLifecycleManager.notifyAppBackgrounded()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    lifecycleState = Lifecycle.State.DESTROYED
                    AppLifecycleManager.updateAppForegroundState(false)
                }

                else -> { /* Handle other events if needed */
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
