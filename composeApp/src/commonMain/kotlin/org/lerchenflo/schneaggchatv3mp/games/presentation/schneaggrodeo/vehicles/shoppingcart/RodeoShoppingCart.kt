package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.shoppingcart

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SnailState
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Shopping cart: an abandoned shopping cart rolls by. Lasso it and the horse shoves it ahead with
// its chest, the cowboy staying in the saddle. The cart plows through every fence in the way, and
// every snail in its path tumbles into the basket - each one counts as caught. At the end the horse
// gives it a last shove and it rolls off, tipping over.

// Shape, shared with the drawing (grid: x from the handle at the rear, y up from the ground)
internal const val CART_LENGTH = 14f
internal const val CART_WHEEL_RADIUS = 0.9f
/** The basket: floor, rim height and its ends. */
internal const val BASKET_FLOOR = 3.5f
internal const val BASKET_TOP = 11f
internal const val BASKET_REAR = 1.5f
internal const val BASKET_FRONT = 13.5f
/** Snails shown in the basket at most; the rest are just counted. */
internal const val CART_MAX_SHOWN_SNAILS = 12

private const val CART_PASS_SPEED = 26f          // u/s across the screen while rolling by, slow enough to lasso
private const val CART_HITCH_Y = 7f
/** Where the loop lands on the cart: the middle of the lasso's reach, ahead of the hand. */
/** Seconds the loop takes to reach the cart (half the lasso's throw), aimed at where it will be. */
/** Handle against the horse's chest while shoving. */
private const val CART_RIDE_X = HORSE_X + 29f
private const val CART_BOARD_SECONDS = 0.6f
private const val CART_RIDE_SECONDS = 8f
private const val CART_FIRST_FENCE_SECONDS = 0.4f
private const val CART_FENCE_SECONDS = 0.7f
private const val CART_RATTLE_DEGREES = 1.5f
private const val WHEEL_TURN = 1.1f              // radians per unit of ground: small casters spin fast
/** Snails up to this high (on low fences, hopping runners) still land in the basket. */
private const val CATCH_HEIGHT = BASKET_TOP + 2f
// The last shove: rolls off to the right, slowing down, and tips over
private const val SHOVE_SPEED = 90f
private const val SHOVE_DRAG = 60f
private const val TIP_DEGREES = 75f
private const val TIP_SECONDS = 0.5f

internal class RodeoShoppingCart : RodeoVehicle(RodeoVehicleKind.SHOPPING_CART) {

    private var boardStartX = 0f
    private var wheelPhase = 0f
    private var rattle = 0f
    private var fenceIn = 0f
    private var snailsInBasket = 0
    private var shoveSpeed = 0f
    private var rotation = 0f

    override val length = CART_LENGTH
    override val passSpeed = CART_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    /** The whole cart is a target, like the Ford Escort. */
    override fun hitch() = hitchX(inset = 1f) to CART_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        snailsInBasket = 0
        rotation = 0f
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        // A jump in progress ends right here; the horse stays on the ground behind the cart
        world.takeHorseOffTheGround()
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        rattle += dt
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                wheelPhase += scroll * WHEEL_TURN
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                wheelPhase += scroll * WHEEL_TURN
                x = lerp(boardStartX, CART_RIDE_X, smoothstep(progressOf(phaseTime, CART_BOARD_SECONDS)))
                if (phaseTime >= CART_BOARD_SECONDS) {
                    enter(VehiclePhase.RIDING)
                    fenceIn = CART_FIRST_FENCE_SECONDS
                }
            }
            VehiclePhase.RIDING -> {
                wheelPhase += scroll * WHEEL_TURN
                fenceIn -= dt
                if (fenceIn <= 0f) {
                    world.addFence(x = world.worldWidth, gapAfter = CART_LENGTH * 2f)
                    fenceIn = CART_FENCE_SECONDS
                }
                scoop(world)
                if (phaseTime >= CART_RIDE_SECONDS) {
                    // The last shove
                    enter(VehiclePhase.LEAVING)
                    shoveSpeed = SHOVE_SPEED
                    world.resumeFences()
                    world.dust(x)
                }
            }
            // Not used: the cowboy never leaves the saddle
            VehiclePhase.UNLOADING -> enter(VehiclePhase.LEAVING)
            VehiclePhase.LEAVING -> {
                // Rolls off, slowing down against the ground, and tips over; gone once out of the picture
                shoveSpeed = max(0f, shoveSpeed - SHOVE_DRAG * dt)
                x += shoveSpeed * dt - scroll
                wheelPhase += shoveSpeed * dt * WHEEL_TURN
                rotation = TIP_DEGREES * smoothstep(progressOf(phaseTime - 0.6f, TIP_SECONDS).coerceAtLeast(0f))
                world.clearTrack(x, x + CART_LENGTH)
                if (x + CART_LENGTH < 0f || x > world.worldWidth + 5f) enter(VehiclePhase.IDLE)
            }
        }
    }

    /** Snails in front of the cart tumble into the basket (caught), then fences are plowed aside. */
    private fun scoop(world: RodeoWorld) {
        world.snails
            .filter { it.state == SnailState.ACTIVE && it.x > x && it.x < x + CART_LENGTH && it.height <= CATCH_HEIGHT }
            .forEach { snail ->
                world.eatSnail(snail)
                snailsInBasket++
            }
        world.clearTrack(x, x + CART_LENGTH) { fence -> world.dust(fence.x) }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val shoving = phase == VehiclePhase.RIDING
        return RodeoShoppingCartUi(
            x = x,
            rotation = if (shoving) CART_RATTLE_DEGREES * sin(rattle * 50f) else rotation,
            wheelPhase = wheelPhase,
            snails = min(snailsInBasket, CART_MAX_SHOWN_SNAILS),
            lassoHint = lassoHint(x + CART_LENGTH / 2f, BASKET_TOP + 5f),
        )
    }
}
