package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import org.maplibre.compose.util.MaplibreComposable

/**
 * Wraps a source + layer block with a stable composition key derived from [layerId].
 *
 * Without a stable key, Compose uses positional identity: when a forEach loop adds or removes
 * items, every subsequent block shifts to a new slot. The new slot's [remember] returns whatever
 * source object was cached there before (a different type or user), the old DisposableEffect
 * disposes and tries to remove that source while a new one tries to add it — causing
 * CannotAddSourceException.
 *
 * Wrapping with key(layerId) gives each source/layer block a stable, named composition slot so
 * it is always matched to the same remembered state regardless of surrounding list changes.
 */
@Composable
@MaplibreComposable
fun safeAdd(
    layerId: String,
    content: @Composable @MaplibreComposable () -> Unit,
) {
    key(layerId) {
        content()
    }
}
