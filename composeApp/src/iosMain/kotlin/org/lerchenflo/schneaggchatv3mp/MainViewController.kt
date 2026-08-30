package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.initKoin
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

fun MainViewController() = ComposeUIViewController(
    configure = {
        // IOSKEYBOARDFIX: the app handles the keyboard itself via WindowInsets.ime
        // (contentWindowInsets on the root Scaffold in App.kt); DoNothing stops Compose's
        // own OffsetToFocusedRect from *also* panning the whole scene up on focus change.
        onFocusBehavior = OnFocusBehavior.DoNothing
        initKoin()
        NotificationManager.initialize()
    }
) {
    App()
}