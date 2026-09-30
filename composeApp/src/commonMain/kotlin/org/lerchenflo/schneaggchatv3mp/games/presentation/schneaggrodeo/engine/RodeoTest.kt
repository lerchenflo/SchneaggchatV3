package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind

// Developer toggles for trying out one vehicle or one map on its own. Leave both null for the real
// game. While a test runs, only what is tested shows up: no wild horses, landscapes, stops,
// Stanislaus or ways between the maps.

internal object RodeoTest {
    /**
     * The only vehicle that comes: right at the start of the run (on its own map) and again shortly
     * after each ride, e.g. `RodeoVehicleKind.DRILL`.
     */
    val vehicle: RodeoVehicleKind? = null

    /** The map the run starts on and stays on, e.g. `RodeoMap.SEA`; the vehicle's map wins if both are set. */
    val map: RodeoMap? = null

    val isActive: Boolean get() = vehicle != null || map != null
}

/** Seconds after a test vehicle left until it comes again. */
internal const val TEST_VEHICLE_AGAIN_SECONDS = 2f
