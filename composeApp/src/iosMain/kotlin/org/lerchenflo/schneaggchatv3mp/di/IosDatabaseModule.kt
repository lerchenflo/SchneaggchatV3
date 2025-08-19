package org.lerchenflo.schneaggchatv3mp.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.iosAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient

val IosDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { iosAppDatabaseBuilder() }
}

val IosHttpModule = module {
    single<HttpClient> {createHttpClient(Darwin.create())}
}