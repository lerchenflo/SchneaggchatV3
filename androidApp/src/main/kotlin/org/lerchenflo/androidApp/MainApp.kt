package org.lerchenflo.androidApp

import android.app.Application
import org.lerchenflo.schneaggchatv3mp.di.logCrash
import org.lerchenflo.schneaggchatv3mp.di.startKoinAndroid
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationConfig

class MainApp: Application() {

    override fun onCreate() {
        super.onCreate()

        NotificationConfig.iconResId = R.drawable.schneaggchat_logo_v3_transparent

        startKoinAndroid(this@MainApp)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        NotificationManager.initialize()
    }
}
