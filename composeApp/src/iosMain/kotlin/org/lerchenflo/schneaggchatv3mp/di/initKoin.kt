package org.lerchenflo.schneaggchatv3mp.di

import org.koin.core.context.startKoin

fun initKoin(){
    startKoin {
        //Userdatabase
        modules(IosDatabaseModule, sharedmodule)

        //Httpclient
        modules(IosHttpModule, IosDatastoreModule)
    }
}