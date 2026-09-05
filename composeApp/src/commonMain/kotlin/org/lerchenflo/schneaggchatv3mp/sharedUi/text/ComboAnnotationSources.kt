package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatformTools
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_title
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_title
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_title
import schneaggchatv3mp.composeapp.generated.resources.games_morse_title
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_title
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_title
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_title
import schneaggchatv3mp.composeapp.generated.resources.games_2048_title
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_title
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title
import schneaggchatv3mp.composeapp.generated.resources.games_yahtzee_title
import schneaggchatv3mp.composeapp.generated.resources.unknown_event
import schneaggchatv3mp.composeapp.generated.resources.unknown_game
import schneaggchatv3mp.composeapp.generated.resources.unknown_location
import schneaggchatv3mp.composeapp.generated.resources.unknown_user
import kotlin.time.Clock

/**
 * Default [ComboAnnotationSource]s, self-contained: names are looked up through the injected
 * repositories and clicks navigate via the [Navigator] singleton, so callers of [ComboText] /
 * [ComboInputField] don't need to provide anything.
 *
 * Extend this list when adding new annotation types (e.g. user mentions).
 */
@Composable
fun rememberComboAnnotationSources(): List<ComboAnnotationSource> {
    // getOrNull keeps @Previews (no Koin context) working — they just render raw text
    val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return emptyList()
    val mapRepository = remember(koin) { koin.get<MapRepository>() }
    val userRepository = remember(koin) { koin.get<UserRepository>() }
    val eventRepository = remember(koin) { koin.get<EventRepository>() }

    val navigator = remember(koin) { koin.get<Navigator>() }
    val scope = rememberCoroutineScope()

    val locationNames by remember(mapRepository) {
        mapRepository.getAllMapEntriesFlow()
            .map { entries -> entries.associate { it.id to it.name } }
    }.collectAsState(initial = emptyMap())

    val userNames by remember(userRepository) {
        userRepository.getAllUsersFlow()
            .map { entries -> entries.associate { it.id to it.name } }
    }.collectAsState(initial = emptyMap())

    val eventNames by remember(eventRepository) {
        eventRepository.getAllEventsFlow()
            .map { entries -> entries.associate { it.id to it.title } }
    }.collectAsState(initial = emptyMap())

    val unknownUser = stringResource(Res.string.unknown_user)
    val unknownLocation = stringResource(Res.string.unknown_location)
    val unknownEvent = stringResource(Res.string.unknown_event)
    val unknownGame = stringResource(Res.string.unknown_game)

    val gameTetrisTitle = stringResource(Res.string.games_tetris_title)
    val gameTowerstackTitle = stringResource(Res.string.games_stack_tower)
    val gameMorseTitle = stringResource(Res.string.games_morse_title)
    val gameSchneaggaHusTitle = stringResource(Res.string.games_schneaggahus_title)
    val gameGridRushTitle = stringResource(Res.string.games_gridrush_title)
    val gameOddOneOutTitle = stringResource(Res.string.games_oddoneout_title)
    val gameCoinFlipTitle = stringResource(Res.string.games_coinflip_title)
    val gameFingerPickerTitle = stringResource(Res.string.games_fingerpicker_title)
    val gameDartCounterTitle = stringResource(Res.string.games_dartcounter_title)
    val gameUndercoverTitle = stringResource(Res.string.games_undercover_title)
    val gameYatziTitle = stringResource(Res.string.games_yahtzee_title)
    val game2048Title = stringResource(Res.string.games_2048_title)
    val gameStanislausTitle = stringResource(Res.string.games_stanislaus_title)

    val gameNames = remember(
        gameTetrisTitle, gameTowerstackTitle, gameMorseTitle, gameSchneaggaHusTitle,
        gameGridRushTitle, gameOddOneOutTitle, gameCoinFlipTitle, gameFingerPickerTitle,
        gameDartCounterTitle, gameUndercoverTitle, gameYatziTitle, game2048Title, gameStanislausTitle
    ) {
        mapOf(
            "tetris" to gameTetrisTitle,
            "towerstack" to gameTowerstackTitle,
            "morse" to gameMorseTitle,
            "schneaggahus" to gameSchneaggaHusTitle,
            "gridrush" to gameGridRushTitle,
            "oddoneout" to gameOddOneOutTitle,
            "coinflip" to gameCoinFlipTitle,
            "fingerpicker" to gameFingerPickerTitle,
            "dartcounter" to gameDartCounterTitle,
            "undercover" to gameUndercoverTitle,
            "yatzi" to gameYatziTitle,
            "2048" to game2048Title,
            "stanislaus" to gameStanislausTitle,
        )
    }

    return remember(
        locationNames, userNames, gameNames, eventNames,
        unknownUser, unknownLocation, unknownEvent, unknownGame
    ) {
        listOf(
            ComboAnnotationSource(
                type = ComboAnnotationTypes.MAP_LOCATION,
                names = locationNames,
                unresolvedName = unknownLocation,
                onClick = { entryId ->
                    scope.launch {
                        navigator.navigate(
                            Route.Schneaggmap(
                                initialEntryId = entryId,
                                openRequestId = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    }
                }
            ),
            ComboAnnotationSource(
                type = ComboAnnotationTypes.USER,
                names = userNames,
                unresolvedName = unknownUser,
                onClick = { entryId ->
                    if (entryId != SessionCache.requireLoggedIn()?.userId) { //Dont allow to open own chat
                        scope.launch { navigator.navigate(Route.Chat(entryId, false)) }
                    }
                }
            ),
            ComboAnnotationSource(
                type = ComboAnnotationTypes.EVENT,
                names = eventNames,
                unresolvedName = unknownEvent,
                onClick = { eventId ->
                    scope.launch { navigator.navigate(Route.Events(selectedEventId = eventId)) }
                }
            ),
            ComboAnnotationSource(
                type = ComboAnnotationTypes.GAME,
                names = gameNames,
                unresolvedName = unknownGame,
                onClick = { gameKey ->
                    val route = when (gameKey) {
                        "tetris" -> Route.Tetris
                        "towerstack" -> Route.TowerStack
                        "morse" -> Route.Morse
                        "schneaggahus" -> Route.SchneaggaHus
                        "gridrush" -> Route.GridRush
                        "oddoneout" -> Route.OddOneOut
                        "coinflip" -> Route.CoinFlip
                        "fingerpicker" -> Route.FingerPicker
                        "dartcounter" -> Route.DartCounter
                        "undercover" -> Route.Undercover
                        "yatzi" -> Route.Yatzi
                        "2048" -> Route.Game2048
                        "stanislaus" -> Route.Stanislaus
                        else -> Route.GamesSelector
                    }
                    scope.launch { navigator.navigate(route) }
                }
            )
        )
    }
}
