package org.lerchenflo.schneaggchatv3mp

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.di.desktopAppDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.desktopAudioManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpAuthModule
import org.lerchenflo.schneaggchatv3mp.di.desktopHttpModule
import org.lerchenflo.schneaggchatv3mp.di.desktopKSafeModule
import org.lerchenflo.schneaggchatv3mp.di.desktopLanguageManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopNotifierModule
import org.lerchenflo.schneaggchatv3mp.di.desktopPermissionManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.desktopShareUtilsModule
import org.lerchenflo.schneaggchatv3mp.di.desktopVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule

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
                desktopPermissionManagerModule,
                desktopAudioManagerModule,
                desktopShareUtilsModule,
                desktopLanguageManagerModule,
                desktopNotifierModule
            )
        }
    }
}