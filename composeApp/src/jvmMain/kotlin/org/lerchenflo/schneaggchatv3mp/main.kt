package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.desktopAppDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.desktopDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpModule
import org.lerchenflo.schneaggchatv3mp.di.desktopPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule
import java.awt.Dimension

fun main() = application {

    startKoin {
        modules(desktopAppDatabaseModule, sharedmodule)

        modules(
            desktopHttpModule,
            desktopDataStoreModule,
            desktopVersionModule,
            desktopPictureManagerModule
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3 Desktop",

    ) {
        window.setSize(1600, 1000)
        window.minimumSize = Dimension(1000, 800)
        App()
    }
}