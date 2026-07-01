package org.lerchenflo.schneaggchatv3mp.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationCredentialsMirror

fun initKoin(){
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            //Userdatabase
            modules(IosDatabaseModule, sharedmodule)

            //Httpclient
            modules(
                IosHttpModule,
                IosHttpAuthModule,
                IosSocketHttpModule,

                IosDatastoreModule,
                IosKSafeModule,

                IosVersionModule,
                IosPictureManagerModule,
                IosPermissionManagerModule,
                IosAudioManagerModule,
                IosShareUtilsModule,
                IosLanguageManagerModule,
                IosNotifierModule,
                IosAppIconManagerModule
            )
        }

        // Seed shared App Group defaults so the Notification Service Extension
        // can localize and decrypt pushes for users who already had the app
        // installed before this version landed.
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val languageService = KoinPlatform.getKoin().get<LanguageService>()
                languageService.applyLanguage(languageService.getCurrentLanguage())

                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                NotificationCredentialsMirror.setEncryptionKey(
                    prefs.getEncryptionKey().ifEmpty { null }
                )
            }
        }
    }
}
