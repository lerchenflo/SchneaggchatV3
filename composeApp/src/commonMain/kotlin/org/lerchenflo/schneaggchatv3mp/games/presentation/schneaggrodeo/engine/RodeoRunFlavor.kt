package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.random.Random

// Every run gets its own weather and time of day, rolled at the start, so no two runs look (or
// play) the same:
//  - rain: more mud puddles on the track; once it stops a rainbow comes along (see RodeoSkyWonders)
//  - snow: fewer puddles, a white dusting on the ground
//  - dusk: a warm evening light; night: dark all around, a lantern lights up the horse
// Together with the shuffled order of the other maps (see RodeoMapSwitch) and the random
// vehicles, landscapes and stops, every run is different.

enum class RodeoWeather { CLEAR, RAIN, SNOW }

enum class RodeoTimeOfDay { DAY, DUSK, NIGHT }

/** Weather chances: mostly clear, sometimes rain or snow. */
private const val RAIN_CHANCE = 0.25f
private const val SNOW_CHANCE = 0.15f
private const val DUSK_CHANCE = 0.25f
private const val NIGHT_CHANCE = 0.2f
/** How much more (or less) mud the weather brings. */
private const val RAIN_MUD_FACTOR = 1.8f
private const val SNOW_MUD_FACTOR = 0.4f
/** Game seconds the rain lasts before it stops. */
private const val RAIN_MIN_SECONDS = 50f
private const val RAIN_RANDOM_SECONDS = 40f

internal class RodeoRunFlavor {
    var weather = RodeoWeather.CLEAR
        private set
    var timeOfDay = RodeoTimeOfDay.DAY
        private set
    /** The rain stopped and the rainbow did not come yet. */
    var rainbowDue = false
        private set
    private var rainLeft = 0f

    /** Multiplies the chance of a mud puddle per fence. */
    val mudFactor: Float
        get() = when (weather) {
            RodeoWeather.CLEAR -> 1f
            RodeoWeather.RAIN -> RAIN_MUD_FACTOR
            RodeoWeather.SNOW -> SNOW_MUD_FACTOR
        }

    /** Rolls a new weather and time of day for a fresh run. */
    fun roll() {
        val weatherRoll = Random.nextFloat()
        weather = when {
            weatherRoll < RAIN_CHANCE -> RodeoWeather.RAIN
            weatherRoll < RAIN_CHANCE + SNOW_CHANCE -> RodeoWeather.SNOW
            else -> RodeoWeather.CLEAR
        }
        rainLeft = RAIN_MIN_SECONDS + Random.nextFloat() * RAIN_RANDOM_SECONDS
        rainbowDue = false
        val timeRoll = Random.nextFloat()
        timeOfDay = when {
            timeRoll < DUSK_CHANCE -> RodeoTimeOfDay.DUSK
            timeRoll < DUSK_CHANCE + NIGHT_CHANCE -> RodeoTimeOfDay.NIGHT
            else -> RodeoTimeOfDay.DAY
        }
    }

    /** Lets the rain run out; returns true in the frame it stops. */
    fun tick(dt: Float): Boolean {
        if (weather != RodeoWeather.RAIN) return false
        rainLeft -= dt
        if (rainLeft > 0f) return false
        weather = RodeoWeather.CLEAR
        rainbowDue = true
        return true
    }

    /** The rainbow came along. */
    fun rainbowSent() {
        rainbowDue = false
    }
}
