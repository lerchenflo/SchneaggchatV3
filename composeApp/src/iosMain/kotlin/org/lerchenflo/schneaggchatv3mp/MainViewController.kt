package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.initKoin
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

fun MainViewController() = ComposeUIViewController(
    configure = {
        // The app handles the keyboard via imePadding(); without this, UIKit additionally
        // shifts the whole view up, leaving a gray strip below the chat input on some devices.
        onFocusBehavior = OnFocusBehavior.DoNothing
        initKoin()
        NotificationManager.initialize()
    }
) {
    App()
}