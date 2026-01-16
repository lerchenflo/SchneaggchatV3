package org.lerchenflo.schneaggchatv3mp.di

import org.koin.core.context.startKoin

fun initKoin(){
    startKoin {
        //Userdatabase
        modules(IosDatabaseModule, sharedmodule)

        //Httpclient
        modules(
            IosHttpModule,
            IosHttpAuthModule,
            IosDatastoreModule,
            IosVersionModule,
            IosPictureManagerModule,
            IosShareUtilsModule,
            IosLanguageManagerModule
        )
    }
}