package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.database.iosAppDatabaseBuilder
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.createAuthenticatedHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.createSocketHttpClient
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.IosAppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.IosAudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.IosLanguageManager
import org.lerchenflo.schneaggchatv3mp.utilities.IosPermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.IosPictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.IosShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.battery.IosBatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.location.IosLocationService
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.IosNotifier
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier

val IosDatabaseModule = module {
    single<RoomDatabase.Builder<AppDatabase>> { iosAppDatabaseBuilder() }
}

//Kept as one module (rather than one per qualifier) - the Koin compiler plugin has generated
//colliding `dsl_single` klib symbols for Kotlin/Native targets when the same qualifier-annotated
//type is bound across separate module {} blocks in this file (see
//https://github.com/InsertKoinIO/koin-compiler-plugin/issues/84).
val IosHttpModule = module {
    single<HttpClient>(named(HTTPCLIENTTYPE.AUTHENTICATED)) { createAuthenticatedHttpClient(Darwin.create(), get()) }
    single<HttpClient>(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)) { createHttpClient(Darwin.create()) }
    single<HttpClient>(named(HTTPCLIENTTYPE.SOCKET)) { createSocketHttpClient(Darwin.create()) }
}

val IosDatastoreModule = module {
    single<DataStore<Preferences>> { iosDatastoreBuilder() }
}

val IosKSafeModule = module {
    single { KSafe() }
}

val IosVersionModule = module {
    single<AppVersion> { IosAppVersion() }
}

val IosPictureManagerModule = module {
    single<PictureManager> { IosPictureManager() }
}

val IosPermissionManagerModule = module {
    single<PermissionManager> { IosPermissionManager() }
    single<LocationService> { IosLocationService() }
    single<BatteryService> { IosBatteryService() }
}


val IosAudioManagerModule = module {
    single<AudioManager> { IosAudioManager() }
}


val IosShareUtilsModule = module {
    single<ShareUtils> { IosShareUtils() }
}

val IosLanguageManagerModule = module {
    single<LanguageManager> { IosLanguageManager(get()) }
}

val IosNotifierModule = module {
    single<Notifier> { IosNotifier(get()) }
}