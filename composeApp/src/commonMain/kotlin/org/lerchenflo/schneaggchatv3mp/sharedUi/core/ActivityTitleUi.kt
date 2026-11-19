package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shared screen title (user pick 2026-11-18, "plain bar"): Material3 top bar, fixed 64 dp, title in one
 * line on the left (22 sp, long titles end with …), same background as the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ActivityTitle(
    title: String = "ActivityTitle",
    onBackClick: () -> Unit = {},
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    // Optional icon buttons at the end of the title row
    actions: @Composable RowScope.() -> Unit = {}
){
    TopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        actions = actions,
        // The app's root Scaffold already pads for the status bar
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
