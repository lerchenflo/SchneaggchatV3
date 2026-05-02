package org.lerchenflo.androidApp

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.di.logCrash
import org.lerchenflo.schneaggchatv3mp.di.startKoinAndroid
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import java.io.PrintWriter
import java.io.StringWriter

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoinAndroid(this@MainApp)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        NotificationManager.initialize()
    }
}
