package org.lerchenflo.androidApp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import org.lerchenflo.schneaggchatv3mp.app.App
import org.lerchenflo.schneaggchatv3mp.utilities.ActivityHolder
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)


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

        ActivityHolder.set(this) // Register this activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }


        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityHolder.clear() // Prevent memory leaks!
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            IncomingDataManager.updateText(sharedText)
        }
    }
}