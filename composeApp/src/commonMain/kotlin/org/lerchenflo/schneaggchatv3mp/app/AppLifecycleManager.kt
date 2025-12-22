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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages app lifecycle state to determine if the app is in foreground/background
 */
object AppLifecycleManager {
    private var _isAppInForeground = mutableStateOf(false)
    
    private val _appResumedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    /**
     * SharedFlow that emits an event every time the app comes into the foreground (ON_RESUME)
     */
    val appResumedEvent: SharedFlow<Unit> = _appResumedEvent.asSharedFlow()
    
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
