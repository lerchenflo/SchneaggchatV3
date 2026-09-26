package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.lifecycle.ViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_2048_description
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_description
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_description
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_description
import schneaggchatv3mp.composeapp.generated.resources.games_morse_description
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_description
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_description
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower_description
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_description
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_description
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_description
import schneaggchatv3mp.composeapp.generated.resources.games_yahtzee_description
import schneaggchatv3mp.composeapp.generated.resources.games_2048_title
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_title
import schneaggchatv3mp.composeapp.generated.resources.games_crossword_title
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_title
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_title
import schneaggchatv3mp.composeapp.generated.resources.games_morse_title
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_title
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_title
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_title
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title
import schneaggchatv3mp.composeapp.generated.resources.games_wordle_title
import schneaggchatv3mp.composeapp.generated.resources.games_yahtzee_title
import schneaggchatv3mp.composeapp.generated.resources.tools_coinflip_description
import schneaggchatv3mp.composeapp.generated.resources.tools_fingerpicker_description
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_description
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_title

//Shared over the whole games nav graph, owns the list of selectable games
class GameSelectorViewModel : ViewModel() {

    val gamesList = listOf(
        GameScreenElement(
            title = Res.string.games_gridrush_title,
            description = Res.string.games_gridrush_description,
            icon = Icons.Default.Dataset,
            route = Route.GridRush,
            inDev = GameId.GRIDRUSH.indev,
            gameId = GameId.GRIDRUSH,
            daily = true
        ),
        GameScreenElement(
            title = Res.string.games_crossword_title,
            description = Res.string.games_crossword_description,
            icon = Icons.Default.Abc,
            route = Route.Crossword,
            inDev = GameId.CROSSWORD.indev,
            gameId = GameId.CROSSWORD
        ),
        GameScreenElement(
            title = Res.string.games_wordle_title,
            description = Res.string.games_wordle_description,
            icon = Icons.Default.Apps,
            route = Route.Wordle,
            inDev = GameId.WORDLE.indev,
            gameId = GameId.WORDLE
        ),





        GameScreenElement(
            title = Res.string.games_schneaggahus_title,
            description = Res.string.games_schneaggahus_description,
            icon = Icons.Default.House,
            route = Route.SchneaggaHus,
            inDev = GameId.SCHNEAGGAHUS.indev,
            gameId = GameId.SCHNEAGGAHUS
        ),
        GameScreenElement(
            title = Res.string.games_tetris_title,
            description = Res.string.games_tetris_description,
            icon = Icons.Default.GridOn,
            route = Route.Tetris,
            inDev = GameId.TETRIS.indev,
            gameId = GameId.TETRIS
        ),
        GameScreenElement(
            title = Res.string.games_stack_tower,
            description = Res.string.games_stack_tower_description,
            icon = Icons.Default.Castle,
            route = Route.TowerStack,
            inDev = GameId.TOWERSTACK.indev,
            gameId = GameId.TOWERSTACK
        ),
        GameScreenElement(
            title = Res.string.games_2048_title,
            description = Res.string.games_2048_description,
            icon = Icons.Default.GridOn,
            route = Route.Game2048,
            inDev = GameId.GAME_2048.indev,
            gameId = GameId.GAME_2048
        ),
        GameScreenElement(
            title = Res.string.games_morse_title,
            description = Res.string.games_morse_description,
            icon = Icons.Default.GraphicEq,
            route = Route.Morse,
            inDev = GameId.MORSE.indev,
            gameId = GameId.MORSE
        ),
        GameScreenElement(
            title = Res.string.games_oddoneout_title,
            description = Res.string.games_oddoneout_description,
            icon = Icons.Default.Search,
            route = Route.OddOneOut,
            inDev = GameId.ODDONEOUT.indev,
            gameId = GameId.ODDONEOUT
        ),




        GameScreenElement(
            title = Res.string.games_dartcounter_title,
            description = Res.string.games_dartcounter_description,
            icon = Icons.Default.AdsClick, // ma darf sich gern was besseres usdenka
            route = Route.DartCounter,
            inDev = GameId.DART_COUNTER.indev,
            gameId = GameId.DART_COUNTER
        ),
        GameScreenElement(
            title = Res.string.games_undercover_title,
            description = Res.string.games_undercover_description,
            icon = Icons.Default.Blind,
            route = Route.Undercover,
            inDev = GameId.UNDERCOVER.indev,
            gameId = GameId.UNDERCOVER
        ),
        GameScreenElement(
            title = Res.string.games_yahtzee_title,
            description = Res.string.games_yahtzee_description,
            icon = Icons.Default.Star,
            route = Route.Yatzi,
            inDev = GameId.YATZI.indev,
            gameId = GameId.YATZI
        ),
    )

    /**
     * Utilities rather than games: nothing here is played, scored or has a leaderboard, so they
     * live behind their own tab instead of a "games without highscores" leftover section.
     */
    val toolsList = listOf(
        GameScreenElement(
            title = Res.string.games_coinflip_title,
            description = Res.string.tools_coinflip_description,
            icon = Icons.Default.MonetizationOn,
            route = Route.CoinFlip,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.games_fingerpicker_title,
            description = Res.string.tools_fingerpicker_description,
            icon = Icons.Default.TouchApp,
            route = Route.FingerPicker,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.tools_fuel_title,
            description = Res.string.tools_fuel_description,
            icon = Icons.Default.LocalGasStation,
            route = Route.FuelCalculator,
            inDev = false
        ),
    )
}
