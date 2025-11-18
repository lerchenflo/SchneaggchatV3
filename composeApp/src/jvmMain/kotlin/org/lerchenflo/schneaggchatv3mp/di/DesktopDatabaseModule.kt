package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.desktopAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager


val desktopAppDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { desktopAppDatabaseBuilder() }
}

val desktopHttpModule = module {
    single<HttpClient>(named("api")) {createHttpClient(OkHttp.create(), get(), true)}
}

val desktopHttpAuthModule = module {
    single<HttpClient>(named("auth")) {createHttpClient(OkHttp.create(), get(), false)}
}

val desktopDataStoreModule = module {
    single<DataStore<Preferences>> { desktopDatastoreBuilder() }
}

val desktopVersionModule = module {
    single { AppVersion() }
}

val desktopPictureManagerModule = module {
    single { PictureManager() }
}