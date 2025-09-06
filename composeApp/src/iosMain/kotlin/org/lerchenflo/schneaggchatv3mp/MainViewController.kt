package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.ComposeUIViewController
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}