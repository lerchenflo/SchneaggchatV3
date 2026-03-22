package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.desktopAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.di.HTTPCLIENTTYPE
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils


val desktopAppDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { desktopAppDatabaseBuilder() }
}

val desktopHttpModule = module {
    single<HttpClient>(named(HTTPCLIENTTYPE.AUTHENTICATED)) {createHttpClient(OkHttp.create(), get(),true)}
}

val desktopHttpAuthModule = module {
    single<HttpClient>(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)) {createHttpClient(OkHttp.create(), get(),false)}
}

val desktopDataStoreModule = module {
    single<DataStore<Preferences>> { desktopDatastoreBuilder() }
}

val desktopKSafeModule = module {
    single<KSafe> { KSafe() }
}

val desktopVersionModule = module {
    single { AppVersion() }
}

val desktopPictureManagerModule = module {
    single { PictureManager() }
}

val desktopPermissionManagerModule = module {
    single { PermissionManager() }
}

val desktopAudioManagerModule = module {
    single { AudioManager() }
}

val desktopShareUtilsModule = module {
    single { ShareUtils() }
}

val desktopLanguageManagerModule = module {
    single { LanguageManager(get()) }
}