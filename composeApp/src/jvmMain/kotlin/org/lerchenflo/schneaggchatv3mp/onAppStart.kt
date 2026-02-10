package org.lerchenflo.schneaggchatv3mp

import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.di.desktopAppDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.desktopDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpAuthModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpModule
import org.lerchenflo.schneaggchatv3mp.di.desktopKSafeModule
import org.lerchenflo.schneaggchatv3mp.di.desktopLanguageManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopShareUtilsModule
import org.lerchenflo.schneaggchatv3mp.di.desktopVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule
import java.io.File

fun onAppStart() {
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            modules(desktopAppDatabaseModule, sharedmodule)

            modules(
                desktopHttpModule,
                desktopHttpAuthModule,

                desktopDataStoreModule,
                desktopKSafeModule,

                desktopVersionModule,
                desktopPictureManagerModule,
                desktopShareUtilsModule,
                desktopLanguageManagerModule
            )
        }
    }


    NotifierManager.initialize(
        NotificationPlatformConfiguration.Desktop(
            showPushNotification = true,
            notificationIconPath = composeDesktopResourcesPath() + File.separator + "schneaggchat_logo_v3.png"
        )
    )

    // Initialize custom notification manager for encrypted payload processing
    //NotificationManager.initialize()

    //AppInitializer.onApplicationStart()
}