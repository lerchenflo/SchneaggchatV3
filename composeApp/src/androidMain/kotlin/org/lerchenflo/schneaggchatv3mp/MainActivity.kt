package org.lerchenflo.schneaggchatv3mp

import android.os.Bundle
import android.service.autofill.UserData
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.UserDao
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.getUserDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dao = getUserDatabase(applicationContext).userDao()
        setContent {
            App(dao)
        }
    }
}
