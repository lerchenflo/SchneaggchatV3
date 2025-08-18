package org.lerchenflo.schneaggchatv3mp.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.desktopUserDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient


val desktopUserDatabaseModule = module {
    single<RoomDatabase.Builder<UserDatabase>> { desktopUserDatabaseBuilder() }
}

val desktopHttpModule = module {
    single<HttpClient> {createHttpClient(OkHttp.create())}
}