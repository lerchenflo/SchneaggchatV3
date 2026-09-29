package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaOvenUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import kotlin.random.Random

// Now and then a wood-fired pizza oven stands by the roadside, a pizza baking in its mouth. Lasso
// the pizza out of it while riding by: the horse eats it, gets all its hearts back and grows one
// level stronger (see RodeoHorse.eatPizza). The oven stands behind the track, so the horse passes
// in front of it without crashing.

/** Game seconds until the first oven, and between two. */
private const val OVEN_FIRST_SECONDS = 50f
private const val OVEN_INTERVAL_MIN = 60f
private const val OVEN_INTERVAL_RANDOM = 45f
/** Size of the oven (grid: x from its left end, y up from the ground). */
internal const val OVEN_WIDTH = 16f
/** Middle of the oven's mouth, where the pizza sits and the lasso grabs it. */
internal const val OVEN_MOUTH_X = 8f
internal const val OVEN_MOUTH_Y = 7f

/** An oven by the roadside; [x] its left end. */
internal class PizzaOven(var x: Float) {
    var hasPizza = true
}

/** A pizza dangling in the lasso's loop on its way to the horse; [x] / [y] its center. */
private class CarriedPizza(var x: Float, var y: Float)

internal class RodeoPizzaOvens {
    val ovens = mutableListOf<PizzaOven>()
    private var carried: CarriedPizza? = null
    private var nextIn = OVEN_FIRST_SECONDS

    fun reset() {
        ovens.clear()
        carried = null
        nextIn = OVEN_FIRST_SECONDS
    }

    /** Sets up an oven at the right edge once one is due and [allowed]. */
    fun tick(dt: Float, worldWidth: Float, allowed: Boolean) {
        if (!allowed) return
        nextIn -= dt
        if (nextIn > 0f) return
        nextIn = OVEN_INTERVAL_MIN + Random.nextFloat() * OVEN_INTERVAL_RANDOM
        ovens.add(PizzaOven(x = worldWidth + 5f))
    }

    /** The ovens stand still on the ground, so they scroll by with it. */
    fun scroll(scroll: Float) {
        ovens.forEach { it.x -= scroll }
        ovens.removeAll { it.x + OVEN_WIDTH < 0f }
    }

    /**
     * The closest oven whose pizza will be in [reach] when the loop arrives in [timeToCatch] (the
     * ground moves by at [groundSpeed]); [onEaten] once the loop brought the pizza to the horse.
     */
    fun lassoGrab(reach: ClosedFloatingPointRange<Float>, timeToCatch: Float, groundSpeed: Float, onEaten: () -> Unit): LassoGrab? {
        val oven = ovens
            .filter { it.hasPizza && it.x + OVEN_MOUTH_X - groundSpeed * timeToCatch in reach }
            .minByOrNull { it.x }
            ?: return null
        return object : LassoGrab {
            override val x: Float get() = oven.x + OVEN_MOUTH_X
            override val y: Float get() = OVEN_MOUTH_Y
            override fun catch(): Boolean {
                if (!oven.hasPizza) return false
                oven.hasPizza = false
                carried = CarriedPizza(x, y)
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                carried?.let {
                    it.x = tipX
                    it.y = tipY
                }
            }
            override fun release() {
                carried = null
                onEaten()
            }
        }
    }

    fun ui(): List<RodeoPizzaOvenUi> = ovens.map { RodeoPizzaOvenUi(x = it.x, hasPizza = it.hasPizza) }

    fun pizzaUi(): RodeoPizzaUi? = carried?.let { RodeoPizzaUi(x = it.x, y = it.y) }
}
