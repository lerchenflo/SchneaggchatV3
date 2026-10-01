package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.min
import kotlin.random.Random

// Every horse has a level (it is gold from the hooves up, the higher the better) and lives (hearts
// on its side). A higher level jumps higher, has more hearts and tires more slowly. Hearts drain
// over time and with every crash (half a heart each); carrots bring them back. Once they are gone the horse throws the
// cowboy off and runs away, and a new horse comes along to be lassoed. Wild horses on the track are
// stronger than the ridden one and come with full hearts - lasso one to switch. The faster the
// run, the better the horses that come along: at top pace only strong ones show up.

internal const val MIN_HORSE_LEVEL = 1
internal const val MAX_HORSE_LEVEL = 5
/** The theme-colored horse of the run's start; every other horse has a coat from HORSE_COATS. */
internal const val DEFAULT_COAT = -1
/** Number of coats in HORSE_COATS (render/RodeoFixedColors). */
internal const val HORSE_COAT_COUNT = 5

/** Hearts of a level-1 horse; every level adds one. */
private const val BASE_LIVES = 3
/**
 * Jump strength added per level above 1: level 5 pushes off 32 % harder and jumps about 1.75 times
 * as high as level 1, so a stronger horse is clearly felt.
 */
private const val JUMP_BOOST_PER_LEVEL = 0.08f
/** Seconds of riding one heart lasts at level 1, and how much longer per level. */
private const val SECONDS_PER_LIFE = 18f
private const val SECONDS_PER_LIFE_PER_LEVEL = 4f
// The weakest level of a horse coming along rises with the pace: level 2 from LEVEL_UP_START_SPEED
// on, one more every LEVEL_UP_SPEED_STEP u/s, up to MAX_TOP_PACE_LEVEL
private const val LEVEL_UP_START_SPEED = 105f
private const val LEVEL_UP_SPEED_STEP = 30f
private const val MAX_TOP_PACE_LEVEL = 4

/** A horse's build: [level], [lives] left (fractions drain away) and its [coat]. */
internal data class RodeoHorseStats(
    val level: Int,
    val lives: Float,
    val coat: Int,
) {
    val maxLives: Int get() = maxLivesOf(level)

    companion object {
        fun maxLivesOf(level: Int): Int = BASE_LIVES + level

        /** The horse a run starts on. */
        val START = RodeoHorseStats(level = MIN_HORSE_LEVEL, lives = maxLivesOf(MIN_HORSE_LEVEL).toFloat(), coat = DEFAULT_COAT)

        /** The weakest level of a horse coming along while the ridden horse runs at [speed]. */
        fun minLevelAt(speed: Float): Int =
            if (speed < LEVEL_UP_START_SPEED) MIN_HORSE_LEVEL
            else min(MAX_TOP_PACE_LEVEL, MIN_HORSE_LEVEL + 1 + ((speed - LEVEL_UP_START_SPEED) / LEVEL_UP_SPEED_STEP).toInt())

        /** A wild horse on the track: one or two levels above [current], always fully rested. */
        fun wild(current: Int): RodeoHorseStats {
            val level = min(MAX_HORSE_LEVEL, current + 1 + Random.nextInt(2))
            return RodeoHorseStats(level = level, lives = maxLivesOf(level).toFloat(), coat = Random.nextInt(HORSE_COAT_COUNT))
        }

        /**
         * The horse that comes along after the last one ran off while the run went at [speed]: at
         * least [minLevelAt] that pace (or one above), with most of its hearts.
         */
        fun replacement(speed: Float): RodeoHorseStats {
            val minLevel = minLevelAt(speed)
            val level = min(MAX_HORSE_LEVEL, minLevel + Random.nextInt(2))
            val max = maxLivesOf(level)
            val lives = Random.nextInt(max - 1, max + 1).toFloat()
            return RodeoHorseStats(level = level, lives = lives, coat = Random.nextInt(HORSE_COAT_COUNT))
        }
    }
}

/** How much stronger a horse of [level] jumps than a level-1 horse. */
internal fun jumpBoostOf(level: Int): Float = 1f + JUMP_BOOST_PER_LEVEL * (level - 1)

/** Seconds one heart lasts while riding a horse of [level]. */
internal fun secondsPerLifeOf(level: Int): Float = SECONDS_PER_LIFE + SECONDS_PER_LIFE_PER_LEVEL * (level - 1)
