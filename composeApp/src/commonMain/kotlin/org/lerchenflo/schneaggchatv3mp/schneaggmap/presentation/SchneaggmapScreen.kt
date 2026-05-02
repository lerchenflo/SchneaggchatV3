package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle

@Composable
fun SchneaggmapScreenRoot(
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<SchneaggmapViewModel>()
    SchneaggmapScreen(
        modifier = modifier,
        onBackClick = viewModel::onBackClick
    )
}

@Composable
fun SchneaggmapScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        ActivityTitle(
            title = "Schneaggmap",
            onBackClick = onBackClick
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Coming soon...")
        }
    }
}
