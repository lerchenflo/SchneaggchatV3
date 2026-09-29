package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

sealed interface SchneaggRodeoAction {
    data object StartGame : SchneaggRodeoAction
    /** Ends the current run without submitting a score and returns to the start screen. */
    data object StopGame : SchneaggRodeoAction
    data object RestartGame : SchneaggRodeoAction
    data object TogglePause : SchneaggRodeoAction
    /** Screen is leaving (back, rotation, tab switch): pause and keep the run for the next visit. */
    data object LeaveGame : SchneaggRodeoAction

    /** One display frame passed; the screen is the clock so the world moves in step with vsync. */
    data class OnFrame(val frameSeconds: Float) : SchneaggRodeoAction
    /** The play area was laid out; [widthPx] / [heightPx] decide how far ahead the rider sees. */
    data class OnWorldSizeChanged(val widthPx: Int, val heightPx: Int) : SchneaggRodeoAction

    data object OnJumpPressed : SchneaggRodeoAction
    data object OnJumpReleased : SchneaggRodeoAction
    /** Right half of the screen / arrow down while flying the plane: steer down. */
    data object OnDivePressed : SchneaggRodeoAction
    data object OnDiveReleased : SchneaggRodeoAction
    data object OnLassoClick : SchneaggRodeoAction
    data object OnSuperJumpClick : SchneaggRodeoAction
    data object OnRocketClick : SchneaggRodeoAction
}

/**
 * An all-time highscore shown as a marker on the track. A friend's ([isFriend]) also comes riding
 * along shortly before it, to be lassoed.
 */
@Immutable
data class RodeoGhostUi(
    val username: String,
    val score: Long,
    val isOwn: Boolean,
    val userId: String = "",
    val isFriend: Boolean = false,
)

/**
 * The friends on the track, loaded once per run: profile [pictures] by user id (friends on their
 * own horse or riding along).
 */
@Immutable
data class RodeoPeopleUi(
    val pictures: Map<String, ImageBitmap> = emptyMap(),
)

/** Everything around the track: HUD, controls and overlays. Changes a few times per second at most. */
@Immutable
data class SchneaggRodeoState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val score: Int = 0,
    val runTimeMillis: Long = 0L,
    val snailsCaught: Int = 0,
    val superJumpCharges: Int = 0,
    /** Collected lucky horseshoes, each absorbs one crash. */
    val luckyCharms: Int = 0,
    /** Thrown off the horse: the lasso button is highlighted, it is the only way back up. */
    val isOnFoot: Boolean = false,
    /** The vehicle being ridden; the controls turn into a hint on how to ride it. */
    val ride: RodeoVehicleKind? = null,
    /** Enough snails saved up for the rocket, and nothing else going on. */
    val rocketReady: Boolean = false,
    /** Shown on the speedometer: the horse's pace, or the vehicle's. */
    val speedKmh: Int = 0,
    /** Banner shown briefly over the track when the rider boards a vehicle. */
    val announcement: RodeoVehicleKind? = null,
    /** The lowest all-time highscore above the current score; null offline or once everything is beaten. */
    val nextToBeat: RodeoGhostUi? = null,
    /** A lassoed friend just joined the ride: the run is paused to tell the player whose highscore it raises now. */
    val friendJoined: String? = null,
) {
    /** Flying (plane, rocket): left half of the play area steers up, right half down. */
    val steers: Boolean get() = ride?.steers == true
}

// Render model of the world, rebuilt every frame by SchneaggRodeoEngine.toFrame. Positions are in world
// units (see engine/RodeoScale); vehicles bring their own render models (vehicles/*/Rodeo*Drawing.kt).

@Immutable
data class RodeoFenceUi(
    val x: Float,
    val width: Float,
    val heightCm: Int,
    val top: Float,
    val colorOffset: Int,
    val knocked: Boolean,
    val poleHeights: List<Float>,
)

/** [x] is the sprite center, [height] its underside above the ground. */
@Immutable
data class RodeoSnailUi(
    val x: Float,
    val height: Float,
    val facingLeft: Boolean,
    val tiltDeg: Float,
)

/** Pose of the horse and rider; pitch/pivot are in the horse's own grid (see drawHorseAndRider). */
@Immutable
data class RodeoHorsePose(
    val height: Float,
    val gaitPhase: Float,
    val airborne: Boolean,
    val riderLean: Float,
    val pitchDegrees: Float,
    val pivotX: Float,
    val pivotY: Float,
    val hindLegScale: Float,
    val frontLegFold: Float,
    /** 0..1: front legs lifted and pawing the air while the horse rears up for the super jump. */
    val frontLegRaise: Float = 0f,
    val hatLift: Float,
    val glow: Float,
    /** 0..1: horse crouched and rider flat on its neck, ducking under a bridge. */
    val duck: Float = 0f,
    /** Floating down under a parachute after the rocket ride. */
    val parachute: Boolean = false,
    /** False while horse and rider sit inside a vehicle (the rocket's dome). */
    val visible: Boolean = true,
    /** Shift of the horse from its riding spot while it paces around riderless. */
    val offsetX: Float = 0f,
    /** False while the cowboy is off the horse; he is drawn as [RodeoCowboyUi] then. */
    val hasRider: Boolean = true,
    /** Index into HORSE_COATS, or -1 for the theme-colored horse the run starts on. */
    val coat: Int = -1,
    /** Colors the horse gold from the hooves up; [lives] of [maxLives] hearts on its side (none if 0). */
    val level: Int = 1,
    val lives: Float = 0f,
    val maxLives: Int = 0,
    /** A friend riding this horse: their profile picture is the rider's head. */
    val riderId: String? = null,
    /** A lassoed friend sitting behind the rider, with their profile picture as head. */
    val passengerId: String? = null,
    /** Size of horse and rider, around the hooves: grown or shrunk by a magic mushroom. */
    val scale: Float = 1f,
)

/**
 * The cowboy on his own after being thrown off: [x] is his center, [height] his feet above the
 * ground, [rotation] degrees clockwise around his middle (90 = flat on his back).
 */
@Immutable
data class RodeoCowboyUi(
    val x: Float,
    val height: Float,
    val rotation: Float,
    val facingLeft: Boolean,
    val hatLift: Float,
)

@Immutable
data class RodeoLassoUi(
    val handX: Float,
    val handY: Float,
    val tipX: Float,
    val tipY: Float,
    /** The snail dangling in the loop on its way back, if the throw caught one. */
    val caught: RodeoSnailUi?,
)

/** A lucky horseshoe floating above the track; [height] is its center above the ground. */
@Immutable
data class RodeoHorseshoeUi(
    val x: Float,
    val height: Float,
    val tiltDeg: Float,
)

/** A carrot floating above the track; [height] is its center above the ground. */
@Immutable
data class RodeoCarrotUi(
    val x: Float,
    val height: Float,
    val tiltDeg: Float,
)

/** A magic mushroom on the ground at [x]; [seed] picks its dots. */
@Immutable
data class RodeoMushroomUi(
    val x: Float,
    val seed: Int,
)

/**
 * A stretch of landscape along the track from [x] over [width]: a mountain (with the cable car's
 * cable) or else a forest. [seed] picks the details.
 */
@Immutable
data class RodeoSectionUi(
    val isMountain: Boolean,
    val x: Float,
    val width: Float,
    val seed: Int,
)

/** A pizza oven by the roadside; [x] its left end, [hasPizza] until the lasso took it out. */
@Immutable
data class RodeoPizzaOvenUi(val x: Float, val hasPizza: Boolean)

/** The pizza dangling in the lasso's loop; [x] / [y] its center. */
@Immutable
data class RodeoPizzaUi(val x: Float, val y: Float)

/** A crystal floating in the cave; [height] is its center above the ground, [hue] picks its color. */
@Immutable
data class RodeoGemUi(
    val x: Float,
    val height: Float,
    val tiltDeg: Float,
    val hue: Int,
)

/**
 * A way to the other map from [x] over [width]: a mine shaft in the track, or ([exit]) the shaft of
 * light leading out of the cave.
 */
@Immutable
data class RodeoPortalUi(
    val x: Float,
    val width: Float,
    val exit: Boolean,
)

/** A mud puddle on the ground from [x] over [width]; [seed] picks its splotches. */
@Immutable
data class RodeoMudUi(
    val x: Float,
    val width: Float,
    val seed: Int,
)

/**
 * Another horse on the track: a wild one or a friend on their own ([friendName], name shown above).
 * [x] is its left edge; [lassoable] shows the "lasso it" hint.
 */
@Immutable
data class RodeoWildHorseUi(
    val x: Float,
    val pose: RodeoHorsePose,
    val friendName: String?,
    val lassoable: Boolean,
)

/** Short burst of sparks where a horseshoe was picked up or a lucky charm absorbed a crash. */
@Immutable
data class RodeoSparkleUi(
    val x: Float,
    val y: Float,
    val progress: Float,
)

@Immutable
data class RodeoDustUi(
    val x: Float,
    val progress: Float,
)

/** A highscore marker post standing on the track at [x]. */
@Immutable
data class RodeoMarkerUi(
    val x: Float,
    val username: String,
    val score: Long,
    val isOwn: Boolean,
    /** Drawn on a shorter post, so the labels of close highscores don't stack up. */
    val staggered: Boolean,
)

@Immutable
data class SchneaggRodeoFrame(
    /** How far the ground has scrolled; moves the pebbles. */
    val distance: Float = 0f,
    val fences: List<RodeoFenceUi> = emptyList(),
    val snails: List<RodeoSnailUi> = emptyList(),
    val pack: List<RodeoSnailUi> = emptyList(),
    val horse: RodeoHorsePose = RodeoHorsePose(
        height = 0f,
        gaitPhase = 0f,
        airborne = false,
        riderLean = 0f,
        pitchDegrees = 0f,
        pivotX = 12f,
        pivotY = 12f,
        hindLegScale = 1f,
        frontLegFold = 0f,
        hatLift = 0f,
        glow = 0f,
    ),
    val lasso: RodeoLassoUi? = null,
    val dust: RodeoDustUi? = null,
    /** 0..1 while the landing splash is shown. */
    val splashProgress: Float? = null,
    val markers: List<RodeoMarkerUi> = emptyList(),
    /** Painted on the horse's side like a race number. */
    val snailsCaught: Int = 0,
    /** Only set while the cowboy is off the horse. */
    val cowboy: RodeoCowboyUi? = null,
    val horseshoes: List<RodeoHorseshoeUi> = emptyList(),
    /** Stored lucky charms; the horse shows a faint aura while it has any. */
    val luckyCharms: Int = 0,
    val sparkle: RodeoSparkleUi? = null,
    val vehicles: List<RodeoVehicleUi> = emptyList(),
    /** A vehicle races: the ground smears into streaks and speed lines fly through the sky. */
    val speedBlur: Boolean = false,
    /** Offset of the whole picture while a vehicle rattles along, in units. */
    val shakeX: Float = 0f,
    val shakeY: Float = 0f,
    /** Units the view is panned up by, following the horse into the sky. */
    val cameraY: Float = 0f,
    /** 0..1: the sky has turned into space (rocket ride). */
    val space: Float = 0f,
    val carrots: List<RodeoCarrotUi> = emptyList(),
    val mud: List<RodeoMudUi> = emptyList(),
    /** The horse wades through mud: brown spray at its hooves. */
    val inMud: Boolean = false,
    val wildHorses: List<RodeoWildHorseUi> = emptyList(),
    val sections: List<RodeoSectionUi> = emptyList(),
    val mushrooms: List<RodeoMushroomUi> = emptyList(),
    /** Degrees the whole track tilts on the mountain: negative uphill, positive downhill. */
    val tiltDegrees: Float = 0f,
    /** A slow-motion mushroom: the picture gets a dreamy tint. */
    val slowMotion: Boolean = false,
    /** Down in the cave: dark backdrop, stalagmites instead of fences. */
    val inCave: Boolean = false,
    val portals: List<RodeoPortalUi> = emptyList(),
    val gems: List<RodeoGemUi> = emptyList(),
    val pizzaOvens: List<RodeoPizzaOvenUi> = emptyList(),
    /** Dangling in the lasso on its way to the horse. */
    val pizza: RodeoPizzaUi? = null,
    /** 0..1 how dark the picture is while switching maps. */
    val fade: Float = 0f,
)
