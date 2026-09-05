package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.navbar_chats
import schneaggchatv3mp.composeapp.generated.resources.navbar_events
import schneaggchatv3mp.composeapp.generated.resources.navbar_games
import schneaggchatv3mp.composeapp.generated.resources.navbar_map
import schneaggchatv3mp.composeapp.generated.resources.settings

data class NavigationBarItemTemplate(
    val id: String,
    val title: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val mobileOnly: Boolean = false,
    val showOnlyWhenSelected: Boolean = false
)

val TOP_LEVEL_DESTINATIONS = mapOf<NavKey, NavigationBarItemTemplate>(
    Route.Events() to NavigationBarItemTemplate(
        id = "bottombar_events_button",
        title = Res.string.navbar_events,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
    ),
    Route.Schneaggmap() to NavigationBarItemTemplate(
        id = "bottombar_map_button",
        title = Res.string.navbar_map,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
        mobileOnly = false
    ),
    Route.ChatSelector to NavigationBarItemTemplate(
        id = "bottombar_chatselector_button",
        title = Res.string.navbar_chats,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
    ),
    Route.GamesSelector to NavigationBarItemTemplate(
        id = "bottombar_games_button",
        title = Res.string.navbar_games,
        selectedIcon = Icons.Filled.Games,
        unselectedIcon = Icons.Outlined.Games,
    ),
    Route.SettingsScreen to NavigationBarItemTemplate(
        id = "bottombar_settings_button",
        title = Res.string.settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        showOnlyWhenSelected = true
    )
)
