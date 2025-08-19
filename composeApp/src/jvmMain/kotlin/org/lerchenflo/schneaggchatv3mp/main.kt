package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.desktopAppDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule

fun main() = application {

    startKoin {
        modules(desktopAppDatabaseModule, sharedmodule)

        modules(desktopHttpModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3mp",
    ) {
        App()
    }
}