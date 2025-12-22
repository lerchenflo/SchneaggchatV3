package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.koin.core.context.startKoin
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.di.desktopAppDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.desktopDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpAuthModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpModule
import org.lerchenflo.schneaggchatv3mp.di.desktopPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule
import java.awt.Dimension
import java.io.File

fun main() = application {

    startKoin {
        modules(desktopAppDatabaseModule, sharedmodule)

        modules(
            desktopHttpModule,
            desktopHttpAuthModule,
            desktopDataStoreModule,
            desktopVersionModule,
            desktopPictureManagerModule
        )
    }


    NotifierManager.initialize(
        NotificationPlatformConfiguration.Desktop(
            showPushNotification = true,
            notificationIconPath = composeDesktopResourcesPath() + File.separator + "schneaggchat_logo_v3.png"
        )
    )

    //AppInitializer.onApplicationStart()


    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3 Desktop",

    ) {
        window.setSize(1600, 1000)
        window.minimumSize = Dimension(400, 400)
        App()
    }
}