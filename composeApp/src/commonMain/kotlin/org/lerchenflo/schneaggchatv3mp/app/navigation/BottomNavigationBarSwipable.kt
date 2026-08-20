package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.onboarding.tapTarget

@Composable
fun BottomAppBarSwipable(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    mobile: Boolean,
    developer: Boolean,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier
    ) {
        TOP_LEVEL_DESTINATIONS.forEach { (key, data) ->

            val selected = key == selectedKey

            if (data.mobileOnly && !mobile) return@forEach //Return if this bottom nav entry is only for mobile and the user is on pc

            if (data.showOnlyWhenSelected && !selected && !developer) return@forEach //Settings are only shown when the user has opened them, do not show otherwise

            NavigationBarItem(
                modifier = Modifier.tapTarget(data.id),
                selected = selected,
                onClick = {
                    onSelectKey(key)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) data.selectedIcon else data.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(data.title)
                    )
                }
            )
        }
    }
}