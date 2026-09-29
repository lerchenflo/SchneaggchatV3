package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Fence
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Snail
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SnailState

/**
 * What a vehicle may read and change in the running world - its only way to talk to the game.
 * Implemented by SchneaggRodeoEngine; positions are in world units (see RodeoScale).
 */
internal interface RodeoWorld {
    val worldWidth: Float
    /** The horse's own pace in u/s. */
    val speed: Float
    val fences: MutableList<Fence>
    val snails: MutableList<Snail>
    val horseshoes: MutableList<Horseshoe>
    /** Height of the horse's hooves from its own jump, without any vehicle lift. */
    val horseHeight: Float
    /** Hop height of the riderless horse while the cowboy is away. */
    val riderlessHop: Float
    /** Where the mountain ahead begins (see RodeoLandscape), or null if there is none. */
    val mountainX: Float?

    /** Places a fence at [x]; [gapAfter] is the free space to the next one (for snails and horseshoes in between). */
    fun addFence(x: Float, gapAfter: Float): Fence
    /** Fences come back with a normal gap after a ride. */
    fun resumeFences()
    fun addBonusPoints(points: Int)
    /** A snail taken off the track by something other than the lasso (the shopping cart scoops it up): counts as caught. */
    fun eatSnail(snail: Snail)
    /** A horseshoe touched by the rider: stores a lucky charm, gives points, sparkles. */
    fun collectHorseshoe(shoe: Horseshoe)
    fun sparkle(x: Float, y: Float)
    fun dust(x: Float)
    fun splash()
    /** Knocks the horse like a fence crash; a lucky charm absorbs it. False if a charm took it. */
    fun crash(penalty: Float): Boolean
    /** Horse boards a vehicle: ends a jump in progress and returns the height it had. */
    fun takeHorseOffTheGround(): Float
    /** The pack falls back to its farthest distance. */
    fun escapePack()
}

/**
 * Sweeps the track between [from] and [to] clear, like a cowcatcher: fences there are knocked over
 * (each one reported to [onFence] first), active snails sent flying - all without any penalty.
 */
internal fun RodeoWorld.clearTrack(from: Float, to: Float, onFence: (Fence) -> Unit = {}) {
    fences.forEach { fence ->
        if (!fence.knocked && fence.overlaps(from, to)) {
            fence.knocked = true
            onFence(fence)
        }
    }
    snails.forEach { snail ->
        if (snail.state == SnailState.ACTIVE && snail.x > from && snail.x < to) snail.knock()
    }
}
