package org.lerchenflo.schneaggchatv3mp.di

import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.UserDao
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase

val sharedmodule = module{
    single {
        //getUserDatabase()
    }
}