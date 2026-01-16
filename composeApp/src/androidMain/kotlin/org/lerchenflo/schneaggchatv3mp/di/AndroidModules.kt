package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.androidAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageManager

val androidUserDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { androidAppDatabaseBuilder(androidContext()) }
}

val androidHttpModule = module {
    single<HttpClient>(named("api")) {createHttpClient(
        engine = OkHttp.create(),
        tokenManager = get(),
        useAuth = true,
    )}
}

val androidHttpAuthModule = module {
    single<HttpClient>(named("auth")) { createHttpClient(
        engine = OkHttp.create(),
        tokenManager = get(),
        useAuth = false
    ) }
}



val androidDataStoreModule = module {
    single<DataStore<Preferences>> { createAndroidDataStore(androidContext()) }
}

val androidVersionModule = module {
    single { AppVersion(androidContext()) }
}

val androidPictureManagerModule = module {
    single { PictureManager(androidContext()) }
}

val androidShareUtilsModule = module {
    single { ShareUtils(androidContext()) }
}

val androidLanguageManagerModule = module {
    single<LanguageManager> { LanguageManager(androidContext(), get()) }
}