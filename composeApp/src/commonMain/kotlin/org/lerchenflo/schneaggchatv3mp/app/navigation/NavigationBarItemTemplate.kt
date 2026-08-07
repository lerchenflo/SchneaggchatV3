package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.navbar_chats
import schneaggchatv3mp.composeapp.generated.resources.navbar_games
import schneaggchatv3mp.composeapp.generated.resources.navbar_map

data class NavigationBarItemTemplate(
    val id: String,
    val title: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Route,
)

val navigationbaritems = listOf(
    NavigationBarItemTemplate(
        id = "map",
        title = Res.string.navbar_map,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
        route = Route.Schneaggmap()
    ),
    NavigationBarItemTemplate(
        id = "chatselector",
        title = Res.string.navbar_chats,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        route = Route.ChatSelector
    ),
    NavigationBarItemTemplate(
        id = "spiele",
        title = Res.string.navbar_games,
        selectedIcon = Icons.Filled.Games,
        unselectedIcon = Icons.Outlined.Games,
        route = Route.Games
    ),
)
