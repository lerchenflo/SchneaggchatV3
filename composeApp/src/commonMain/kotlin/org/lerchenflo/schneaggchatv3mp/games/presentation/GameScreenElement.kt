package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route


// Des isch für an GameSelector zum des hoffentlich a kle cleaner macha
data class GameScreenElement(
    val title: String,
    val description: String? = null,
    val icon: ImageVector,
    val route: Route,
    val inDev : Boolean
)