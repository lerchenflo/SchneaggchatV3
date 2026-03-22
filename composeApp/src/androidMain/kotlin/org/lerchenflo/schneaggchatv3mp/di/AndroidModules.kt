package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.androidAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.di.HTTPCLIENTTYPE
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils

val androidUserDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { androidAppDatabaseBuilder(androidContext()) }
}

val androidHttpModule = module {
    single<HttpClient>(named(HTTPCLIENTTYPE.AUTHENTICATED)) {createHttpClient(
        engine = OkHttp.create(),
        tokenManager = get(),
        useAuth = true,
    )}
}

val androidHttpAuthModule = module {
    single<HttpClient>(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)) { createHttpClient(
        engine = OkHttp.create(),
        tokenManager = get(),
        useAuth = false
    ) }
}



val androidDataStoreModule = module {
    single<DataStore<Preferences>> { createAndroidDataStore(androidContext()) }
}

val androidKsafeModule = module {
    single { KSafe(androidContext()) }
}

val androidVersionModule = module {
    single { AppVersion(androidContext()) }
}

val androidPictureManagerModule = module {
    single { PictureManager(androidContext()) }
}

val androidPermissionManagerModule = module {
    single { PermissionManager(androidContext()) }
}

val androidAudioManagerModule = module {
    single { AudioManager(androidContext()) }
}

val androidShareUtilsModule = module {
    single { ShareUtils(androidContext()) }
}

val androidLanguageManagerModule = module {
    single<LanguageManager> { LanguageManager(androidContext(), get()) }
}