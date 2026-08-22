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

fun getVisibleTopLevelDestinations(
    selectedKey: NavKey,
    mobile: Boolean,
    developer: Boolean
): List<NavKey> {
    return TOP_LEVEL_DESTINATIONS.filter { (key, data) ->
        val selected = key == selectedKey || key::class == selectedKey::class
        if (data.mobileOnly && !mobile) return@filter false
        if (data.showOnlyWhenSelected && !selected && !developer) return@filter false
        true
    }.map { it.key }
}

@Composable
fun BottomAppBarSwipable(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    mobile: Boolean,
    developer: Boolean,
    modifier: Modifier = Modifier
) {
    val visibleDestinations = getVisibleTopLevelDestinations(
        selectedKey = selectedKey,
        mobile = mobile,
        developer = developer
    )

    BottomAppBar(
        modifier = modifier
    ) {
        visibleDestinations.forEach { key ->
            val data = TOP_LEVEL_DESTINATIONS[key] ?: return@forEach
            val selected = key == selectedKey || key::class == selectedKey::class

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