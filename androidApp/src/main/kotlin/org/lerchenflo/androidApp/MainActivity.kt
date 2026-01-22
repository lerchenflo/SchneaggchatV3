package org.lerchenflo.androidApp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import org.lerchenflo.schneaggchatv3mp.androidApp.R
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)



        NotifierManager.onCreateOrOnNewIntent(this.intent)


        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Check for updates
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    try {
                        val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                            .build()

                        appUpdateManager.startUpdateFlow(
                            appUpdateInfo,
                            this@MainActivity,
                            options
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }


        //Noti permission
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()


        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }
}