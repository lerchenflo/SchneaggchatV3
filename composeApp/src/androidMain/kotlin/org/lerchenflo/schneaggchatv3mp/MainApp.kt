package org.lerchenflo.schneaggchatv3mp

import android.app.Application
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.lerchenflo.schneaggchatv3mp.di.androidDataStoreModule
import org.lerchenflo.schneaggchatv3mp.di.androidHttpAuthModule
import org.lerchenflo.schneaggchatv3mp.di.androidHttpModule
import org.lerchenflo.schneaggchatv3mp.di.androidPictureManagerModule
import org.lerchenflo.schneaggchatv3mp.di.androidUserDatabaseModule
import org.lerchenflo.schneaggchatv3mp.di.androidVersionModule
import org.lerchenflo.schneaggchatv3mp.di.sharedmodule

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
                androidPictureManagerModule
            )


        }

        //Firebase init
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = true
            )
        )

        NotifierManager.addListener(object : NotifierManager.Listener {
            override fun onPayloadData(data: PayloadData) {
                println("Push Notification payloadData from android side(MainApp.kt): $data") //PayloadData is just typeAlias for Map<String,*>.
            }
        })


        println("Android firebase init fertig")




    }
}