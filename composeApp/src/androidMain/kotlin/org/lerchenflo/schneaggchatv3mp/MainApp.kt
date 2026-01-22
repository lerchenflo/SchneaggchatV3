package org.lerchenflo.schneaggchatv3mp

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.androidApp.R
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.di.androidDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.androidHttpAuthModule
import org.lerchenflo.schneaggchatv3mp.di.androidHttpModule
import org.lerchenflo.schneaggchatv3mp.di.androidLanguageManagerModule
import org.lerchenflo.schneaggchatv3mp.di.androidPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.androidShareUtilsModule
import org.lerchenflo.schneaggchatv3mp.di.androidUserDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.androidVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import java.io.PrintWriter
import java.io.StringWriter

class MainApp: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin{
            //androidLogger(Level.DEBUG) // Enable Koin logging
            androidContext(this@MainApp)

            //Modules für room Userdatabase
            modules(androidUserDatabaseModule, sharedmodule)

            //Modules für Httpclient
            modules(
                androidHttpModule,
                androidHttpAuthModule,
                androidDataStoreModule,
                androidVersionModule,
                androidPictureManagerModule,
                androidShareUtilsModule,
                androidLanguageManagerModule
            )
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

            val loggingRepository = KoinPlatform.getKoin().get<LoggingRepository>()
            runBlocking {
                loggingRepository.log(
                    message = getFullStackTrace(throwable),
                    logType = LogType.ERROR
                )
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }

        //Firebase init
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = org.lerchenflo.androidApp.R.drawable.ic_schne,
                showPushNotification = true
            )
        )

        println("Logo resid: $")

        //Initialize notificationmanager to catch payload in common code
        NotificationManager.initialize()

        println("Android firebase init fertig")


    }

    private fun getFullStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)

        // Print this throwable
        throwable.printStackTrace(pw)

        // Walk through causes
        var cause = throwable.cause
        while (cause != null) {
            pw.println("Caused by:")
            cause.printStackTrace(pw)
            cause = cause.cause
        }

        // Include suppressed
        val suppressed = throwable.suppressed
        if (suppressed.isNotEmpty()) {
            pw.println("Suppressed exceptions:")
            for (sup in suppressed) {
                sup.printStackTrace(pw)
            }
        }

        pw.flush()
        return sw.toString()
    }

}
