package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.lifecycle.ViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_title
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_title
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_title
import schneaggchatv3mp.composeapp.generated.resources.games_morse_title
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_title
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_title
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_title
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title
import schneaggchatv3mp.composeapp.generated.resources.games_yahtzee_title

//Shared over the whole games nav graph, owns the list of selectable games
class GameSelectorViewModel : ViewModel() {

    val gamesList = listOf(
        GameScreenElement(
            title = Res.string.games_tetris_title,
            icon = Icons.Default.Menu, // Placeholder
            route = Route.Games.Tetris,
            inDev = false,
            gameId = GameId.TETRIS
        ),
        GameScreenElement(
            title = Res.string.games_stack_tower,
            icon = Icons.Default.Menu,
            route = Route.Games.TowerStack,
            inDev = false,
            gameId = GameId.TOWERSTACK
        ),
        GameScreenElement(
            title = Res.string.games_morse_title,
            icon = Icons.Default.GraphicEq,
            route = Route.Games.Morse,
            inDev = false,
            gameId = GameId.MORSE
        ),
        GameScreenElement(
            title = Res.string.games_schneaggahus_title,
            icon = Icons.Default.House,
            route = Route.Games.SchneaggaHus,
            inDev = true,
            gameId = GameId.SCHNEAGGAHUS
        ),
        GameScreenElement(
            title = Res.string.games_gridrush_title,
            icon = Icons.Default.GridOn,
            route = Route.Games.GridRush,
            inDev = false,
            gameId = GameId.GRIDRUSH,
            daily = true
        ),
        GameScreenElement(
            title = Res.string.games_oddoneout_title,
            icon = Icons.Default.Search,
            route = Route.Games.OddOneOut,
            inDev = false,
            gameId = GameId.ODDONEOUT
        ),
        GameScreenElement(
            title = Res.string.games_coinflip_title,
            icon = Icons.Default.MonetizationOn,
            route = Route.Games.CoinFlip,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.games_fingerpicker_title,
            icon = Icons.Default.TouchApp,
            route = Route.Games.FingerPicker,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.games_dartcounter_title,
            icon = Icons.Default.AdsClick, // ma darf sich gern was besseres usdenka
            route = Route.Games.DartCounter,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.games_undercover_title,
            icon = Icons.Default.Blind,
            route = Route.Games.Undercover,
            inDev = false
        ),
        GameScreenElement(
            title = Res.string.games_yahtzee_title,
            icon = Icons.Default.Star,
            route = Route.Games.YatziSetup,
            inDev = false
        ),
    )
}
