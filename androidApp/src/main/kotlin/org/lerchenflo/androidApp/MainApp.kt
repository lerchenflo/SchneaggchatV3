package org.lerchenflo.androidApp

import android.app.Application
import androidx.compose.ui.graphics.Color
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.runBlocking
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.di.logCrash
import org.lerchenflo.schneaggchatv3mp.di.startKoinAndroid
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import java.io.PrintWriter
import java.io.StringWriter

class MainApp: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoinAndroid(this@MainApp)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

            logCrash(throwable)

            defaultHandler?.uncaughtException(thread, throwable)
        }

        //Firebase init
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.schneaggchat_logo_v3_transparent,
                showPushNotification = true
            )
        )

        //Initialize notificationmanager to catch payload in common code
        NotificationManager.initialize()

        println("Android firebase init fertig")


    }



}