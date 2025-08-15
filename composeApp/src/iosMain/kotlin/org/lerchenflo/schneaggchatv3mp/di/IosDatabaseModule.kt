package org.lerchenflo.schneaggchatv3mp.di

import androidx.room.RoomDatabase
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.iosUserDatabaseBuilder

val IosDatabaseModule = module {
    single<RoomDatabase.Builder<UserDatabase>> { iosUserDatabaseBuilder() }
}