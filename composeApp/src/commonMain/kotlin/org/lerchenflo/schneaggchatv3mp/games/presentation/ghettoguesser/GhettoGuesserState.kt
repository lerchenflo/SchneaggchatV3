package org.lerchenflo.schneaggchatv3mp.games.presentation.ghettoguesser

import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.games.domain.CommonsPhoto
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessMap
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessRoundResult
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

sealed interface GhettoGuesserAction {
    data class SelectMap(val map: GuessMap) : GhettoGuesserAction
    data object BackToMaps : GhettoGuesserAction
    data class ShowGuessMap(val visible: Boolean) : GhettoGuesserAction
    data class PlaceGuess(val location: LatLong) : GhettoGuesserAction
    data object SubmitGuess : GhettoGuesserAction
    data object SkipPhoto : GhettoGuesserAction
    data object NextRound : GhettoGuesserAction
    data object PlayAgain : GhettoGuesserAction
    data object RetryLoad : GhettoGuesserAction
    data class ShowCredits(val visible: Boolean) : GhettoGuesserAction
}

enum class GhettoGuesserPhase {
    MAP_SELECTION,
    LOADING,
    GUESSING,
    ROUND_RESULT,
    GAME_OVER,
    LOAD_FAILED,
}

enum class PhotoLoadError {
    NO_NETWORK,
    NO_PHOTOS,
}

data class GhettoGuesserState(
    val maps: List<GuessMap> = emptyList(),
    val selectedMap: GuessMap? = null,
    val phase: GhettoGuesserPhase = GhettoGuesserPhase.MAP_SELECTION,
    val loadError: PhotoLoadError? = null,
    /** 1-based number of the round being played. */
    val round: Int = 1,
    val photo: CommonsPhoto? = null,
    val imageBytes: ByteArray? = null,
    val guess: LatLong? = null,
    /** While guessing: map on screen instead of the photo. */
    val isGuessMapVisible: Boolean = false,
    val isCreditsVisible: Boolean = false,
    val results: List<GuessRoundResult> = emptyList(),
    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl,
) {
    val totalPoints: Int
        get() = results.sumOf { it.points }

    val totalTimeMillis: Long
        get() = results.sumOf { it.timeMillis }

    val lastResult: GuessRoundResult?
        get() = results.lastOrNull()
}
