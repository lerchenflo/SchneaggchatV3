package org.lerchenflo.schneaggchatv3mp.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.androidUserDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient

val androidUserDatabaseModule = module {
    single<RoomDatabase.Builder<UserDatabase>> { androidUserDatabaseBuilder(androidContext()) }
}




val androidHttpModule = module {
    single<HttpClient> {createHttpClient(OkHttp.create())}
}