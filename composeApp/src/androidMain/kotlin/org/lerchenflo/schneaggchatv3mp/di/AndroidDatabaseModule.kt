package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.androidAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient

val androidUserDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { androidAppDatabaseBuilder(androidContext()) }
}

val androidHttpModule = module {
    single<HttpClient> {createHttpClient(OkHttp.create())}
}

val androidDataStoreModule = module {
    single<DataStore<Preferences>> { createAndroidDataStore(androidContext()) }
}