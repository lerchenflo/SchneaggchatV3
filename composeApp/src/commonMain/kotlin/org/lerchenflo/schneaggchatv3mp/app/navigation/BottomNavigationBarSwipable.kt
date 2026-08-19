package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.stringResource
import org.koin.viewmodel.lazyResolveViewModel

@Composable
fun BottomAppBarSwipable(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    mobile: Boolean,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier
    ) {
        TOP_LEVEL_DESTINATIONS.forEach { (key, data) ->

            if (data.mobileOnly && !mobile) return@forEach

            val selected = key == selectedKey
            NavigationBarItem(
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