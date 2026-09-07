package org.lerchenflo.schneaggchatv3mp.car

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A [CoroutineScope] cancelled when this owner's lifecycle is destroyed - the car-app equivalent
 * of `LifecycleOwner.lifecycleScope`, built on plain `Lifecycle`/`LifecycleEventObserver` (no
 * `lifecycle-runtime-ktx` dependency, whose presence on this module's androidMain classpath isn't
 * guaranteed) since [androidx.car.app.Session] and [androidx.car.app.Screen] only implement
 * [LifecycleOwner] and not `ViewModelStoreOwner`/`LifecycleCoroutineScope` helpers themselves.
 */
fun LifecycleOwner.newLifecycleScope(): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) scope.cancel()
    })
    return scope
}
