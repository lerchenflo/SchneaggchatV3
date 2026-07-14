package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId


// Des isch für an GameSelector zum des hoffentlich a kle cleaner macha
data class GameScreenElement(
    val title: StringResource,
    val description: StringResource? = null,
    val icon: ImageVector,
    val route: Route,
    val inDev : Boolean,
    // Set for games with a server leaderboard: enables the highscore button + difficulty selection
    val gameId: GameId? = null,
    // Daily games get their own section at the top of the selector
    val daily: Boolean = false
)