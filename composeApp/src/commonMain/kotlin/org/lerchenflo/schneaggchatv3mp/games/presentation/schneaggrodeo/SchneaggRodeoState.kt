package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.runtime.Immutable

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
}

enum class RodeoAnnouncement { CRASH_PILOT, LAWN_TRACTOR }

/** An all-time highscore shown as a marker on the track. */
@Immutable
data class RodeoGhostUi(
    val username: String,
    val score: Long,
    val isOwn: Boolean,
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
    /** In the plane: left half of the play area steers up, right half down. */
    val isFlying: Boolean = false,
    /** Shown on the speedometer: the horse's pace, or the lawn tractor's absurd one. */
    val speedKmh: Int = 0,
    /** Banner shown briefly over the track when the cowboy boards the plane or the tractor. */
    val announcement: RodeoAnnouncement? = null,
    /** The lowest all-time highscore above the current score; null offline or once everything is beaten. */
    val nextToBeat: RodeoGhostUi? = null,
)

// Render model of the world, rebuilt every frame. Positions are in world units (see SchneaggRodeoEngine).

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
    /** Shift of the horse from its riding spot while it paces around riderless. */
    val offsetX: Float = 0f,
    /** False while the cowboy is off the horse; he is drawn as [RodeoCowboyUi] then. */
    val hasRider: Boolean = true,
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

/**
 * The plane, facing right. [x] is the left edge of the fuselage, [y] its underside above the ground,
 * [rotation] degrees clockwise while the wreck tumbles.
 */
@Immutable
data class RodeoPlaneUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val propellerPhase: Float,
    /** The cowboy sits in the cockpit. */
    val hasPilot: Boolean,
    /** Rope ladder hanging down while it passes by and while he climbs up. */
    val ladderDown: Boolean,
)

/**
 * The red lawn tractor, facing right. [x] is its left edge; it stands on the ground and tips over by
 * [rotation] degrees (counterclockwise, around its rear wheel) once wrecked. Parts come off in the
 * order exhaust, steering wheel, hood, mower deck, front wheel: the first [partsLost] are gone.
 */
@Immutable
data class RodeoTractorUi(
    val x: Float,
    val rotation: Float,
    val wheelPhase: Float,
    val partsLost: Int,
    val wrecked: Boolean,
    /** Puffing exhaust while it races. */
    val exhaust: Boolean,
)

/** A part torn off the tractor; [part] indexes the parts of [RodeoTractorUi], anything above is scrap. */
@Immutable
data class RodeoDebrisUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val part: Int,
)

/** A building of the skyline flown over by the plane; [x] is its left edge. */
@Immutable
data class RodeoBuildingUi(
    val x: Float,
    val width: Float,
    val height: Float,
    /** Picks the pattern of lit windows. */
    val seed: Int,
    /** Underside of the storm cloud hanging above, or null for open sky. */
    val cloudBottom: Float? = null,
    /** How far the cloud reaches past the building on each side. */
    val cloudOverhang: Float = 0f,
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
    val plane: RodeoPlaneUi? = null,
    val buildings: List<RodeoBuildingUi> = emptyList(),
    val sparkle: RodeoSparkleUi? = null,
    val tractor: RodeoTractorUi? = null,
    val debris: List<RodeoDebrisUi> = emptyList(),
    /** The tractor races: the ground smears into streaks and speed lines fly through the sky. */
    val speedBlur: Boolean = false,
    /** Offset of the whole picture while the tractor rattles along, in units. */
    val shakeX: Float = 0f,
    val shakeY: Float = 0f,
)
