package org.lerchenflo.schneaggchatv3mp.di

import androidx.room.RoomDatabase
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.androidUserDatabaseBuilder

val androidDatabaseModule = module {
    single<RoomDatabase.Builder<UserDatabase>> { androidUserDatabaseBuilder(androidContext()) }
}