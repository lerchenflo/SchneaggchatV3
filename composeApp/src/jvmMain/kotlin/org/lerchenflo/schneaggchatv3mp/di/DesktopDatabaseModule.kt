package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.desktopAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient


val desktopAppDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { desktopAppDatabaseBuilder() }
}

val desktopHttpModule = module {
    single<HttpClient> {createHttpClient(OkHttp.create())}
}

val desktopDataStoreModule = module {
    single<DataStore<Preferences>> { desktopDatastoreBuilder() }
}