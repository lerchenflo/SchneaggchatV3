package org.lerchenflo.schneaggchatv3mp.di

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initKoin(){
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            //Userdatabase
            modules(IosDatabaseModule, sharedmodule)

            //Httpclient
            modules(
                IosHttpModule,
                IosHttpAuthModule,

                IosDatastoreModule,
                IosKSafeModule,

                IosVersionModule,
                IosPictureManagerModule,
                IosPermissionManagerModule,
                IosAudioManagerModule,
                IosShareUtilsModule,
                IosLanguageManagerModule
            )
        }
    }
}