package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.min
import kotlin.random.Random

// Every horse has a level (it is gold from the hooves up, the higher the better) and lives (hearts
// on its side). A higher level jumps higher, has more hearts and tires more slowly. Hearts drain
// over time and with every crash; carrots bring them back. Once they are gone the horse throws the
// cowboy off and runs away, and a new horse comes along to be lassoed. Wild horses on the track are
// stronger than the ridden one and come with full hearts - lasso one to switch.

internal const val MIN_HORSE_LEVEL = 1
internal const val MAX_HORSE_LEVEL = 5
/** The theme-colored horse of the run's start; every other horse has a coat from HORSE_COATS. */
internal const val DEFAULT_COAT = -1
/** Number of coats in HORSE_COATS (render/RodeoFixedColors). */
internal const val HORSE_COAT_COUNT = 5

/** Hearts of a level-1 horse; every level adds one. */
private const val BASE_LIVES = 2
/**
 * Jump strength added per level above 1: level 5 pushes off 32 % harder and jumps about 1.75 times
 * as high as level 1, so a stronger horse is clearly felt.
 */
private const val JUMP_BOOST_PER_LEVEL = 0.08f
/** Seconds of riding one heart lasts at level 1, and how much longer per level. */
private const val SECONDS_PER_LIFE = 18f
private const val SECONDS_PER_LIFE_PER_LEVEL = 4f
/** A horse coming along after the last one ran off: sometimes it is nearly done for. */
private const val REPLACEMENT_WEAK_CHANCE = 0.3f

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

        /** A wild horse on the track: one or two levels above [current], always fully rested. */
        fun wild(current: Int): RodeoHorseStats {
            val level = min(MAX_HORSE_LEVEL, current + 1 + Random.nextInt(2))
            return RodeoHorseStats(level = level, lives = maxLivesOf(level).toFloat(), coat = Random.nextInt(HORSE_COAT_COUNT))
        }

        /** The horse that comes along after the last one ran off: any level, maybe close to the end. */
        fun replacement(): RodeoHorseStats {
            val level = Random.nextInt(MIN_HORSE_LEVEL, MAX_HORSE_LEVEL - 1)
            val max = maxLivesOf(level)
            val lives = if (Random.nextFloat() < REPLACEMENT_WEAK_CHANCE) 1f else Random.nextInt(2, max + 1).toFloat()
            return RodeoHorseStats(level = level, lives = lives, coat = Random.nextInt(HORSE_COAT_COUNT))
        }
    }
}

/** How much stronger a horse of [level] jumps than a level-1 horse. */
internal fun jumpBoostOf(level: Int): Float = 1f + JUMP_BOOST_PER_LEVEL * (level - 1)

/** Seconds one heart lasts while riding a horse of [level]. */
internal fun secondsPerLifeOf(level: Int): Float = SECONDS_PER_LIFE + SECONDS_PER_LIFE_PER_LEVEL * (level - 1)
