package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.lerchenflo.schneaggchatv3mp.di.desktopUserDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule

fun main() = application {

    startKoin {
        modules(desktopUserDatabaseModule, sharedmodule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3mp",
    ) {
        App()
    }
}