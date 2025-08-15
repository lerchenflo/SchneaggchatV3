package org.lerchenflo.schneaggchatv3mp.di

import org.koin.core.context.startKoin

fun initKoin(){
    startKoin {
        modules(IosDatabaseModule, sharedmodule)
    }
}