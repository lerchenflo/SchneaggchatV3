package org.lerchenflo.schneaggchatv3mp.car

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Minimal Lifecycle/ViewModelStore/SavedStateRegistry owner trio for hosting a `ComposeView`
 * inside the car's [android.app.Presentation] - the same three things `ComponentActivity`
 * provides for free, which `ComposeView.setContent {}` requires and a bare [androidx.car.app.Screen]
 * doesn't implement on its own.
 *
 * Owns its own [LifecycleRegistry] rather than delegating to the [androidx.car.app.Screen]'s
 * lifecycle: [SavedStateRegistryController.performRestore] requires its owner's lifecycle to
 * still be in [Lifecycle.State.INITIALIZED], which the Screen's lifecycle no longer is by the
 * time [android.app.Presentation]'s Surface actually becomes available (it's created well after
 * the Screen itself).
 */
class CarComposeOwners : SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Call when the hosting [android.app.Presentation] is torn down (surface destroyed). */
    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
