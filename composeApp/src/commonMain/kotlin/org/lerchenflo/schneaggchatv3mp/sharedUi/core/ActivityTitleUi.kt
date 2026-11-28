package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.more_options

/**
 * Shared screen title (user pick 2026-11-18, "plain bar"): Material3 top bar, fixed 64 dp, title in one
 * line on the left (22 sp), same background as the screen. A long title scrolls sideways (user, 2026-11-24;
 * before it ended with …).
 *
 * Screen actions go into [menuItems], the ⋮ at the end (only shown while the list isn't empty).
 * [actions] is for single icon buttons before it, e.g. a toggle, or a [TitleMenuButton] with its
 * own icon for a choice like the sort order.
 */
@Preview
@Composable
fun ActivityTitle(
    title: String = "ActivityTitle",
    onBackClick: () -> Unit = {},
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    // Optional icon buttons at the end of the title row, before the ⋮
    actions: @Composable RowScope.() -> Unit = {},
    menuItems: List<TitleMenuItem> = emptyList(),
    menuButtonModifier: Modifier = Modifier,
) = ActivityTitle(
    title = AnnotatedString(title),
    onBackClick = onBackClick,
    showBackButton = showBackButton,
    modifier = modifier,
    backButtonModifier = backButtonModifier,
    actions = actions,
    menuItems = menuItems,
    menuButtonModifier = menuButtonModifier,
)

/** [ActivityTitle] with a styled title, e.g. a name with a smaller italic part. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTitle(
    title: AnnotatedString,
    onBackClick: () -> Unit = {},
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    menuItems: List<TitleMenuItem> = emptyList(),
    menuButtonModifier: Modifier = Modifier,
){
    TopAppBar(
        title = {
            // A title too long for the space next to the back button and actions scrolls sideways
            // instead of being cut off
            Text(
                text = title,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        },
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            if (showBackButton) {
                BackButton(
                    onBackClick = onBackClick,
                    modifier = backButtonModifier,
                    contentPadding = PaddingValues(0.dp)
                )
            }
        },
        actions = {
            actions()
            if (menuItems.isNotEmpty()) {
                TitleMenuButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.more_options),
                    items = menuItems,
                    modifier = menuButtonModifier
                )
            }
        },
        // The app's root Scaffold already pads for the status bar
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
