package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.iosAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageManager

val IosDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { iosAppDatabaseBuilder() }
}

val IosHttpModule = module {
    single<HttpClient>(named("api")) {createHttpClient(Darwin.create(), get(), true)}
}

val IosHttpAuthModule = module {
    single<HttpClient>(named("auth")) {createHttpClient(Darwin.create(), get(), false)}
}

val IosDatastoreModule = module {
    single<DataStore<Preferences>> { iosDatastoreBuilder() }
}

val IosVersionModule = module {
    single { AppVersion() }
}

val IosPictureManagerModule = module {
    single { PictureManager() }
}

val IosShareUtilsModule = module {
    single { ShareUtils() }
}

val IosLanguageManagerModule = module {
    single { LanguageManager(get()) }
}