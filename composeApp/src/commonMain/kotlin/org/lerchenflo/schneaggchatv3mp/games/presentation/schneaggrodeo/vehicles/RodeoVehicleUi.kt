package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer

/**
 * Render model of a vehicle, rebuilt every frame by [RodeoVehicle.ui]. Each one draws itself, layer
 * by layer, so the track canvas never needs to know which vehicles exist.
 */
interface RodeoVehicleUi {
    /** Where the "lasso it" hint points to while it passes by. */
    val lassoHint: RodeoLassoHintUi?

    /**
     * Where it stands on the ground, if it is rigid: it draws itself on it (see onFootprint), and a
     * horse standing on it tilts along with it.
     */
    val footprint: RodeoFootprint? get() = null

    /** Draws the parts of the vehicle that belong on [layer] (see [RodeoLayer]). */
    fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext)
}

/** Anchor of the "lasso it" hint: [x] is its center, [y] the tip of its pointer above the ground. */
@Immutable
data class RodeoLassoHintUi(val x: Float, val y: Float)
