package org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AutoLoginCredCheckerRoot(
    viewModel: AutoLoginCredCheckerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AutoLoginCredCheckerScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun AutoLoginCredCheckerScreen(
    state: AutoLoginCredCheckerState,
    onAction: (AutoLoginCredCheckerAction) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
        }
    }
}
