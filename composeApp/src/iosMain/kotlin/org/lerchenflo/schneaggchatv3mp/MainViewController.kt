package org.lerchenflo.schneaggchatv3mp

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import org.lerchenflo.schneaggchatv3mp.database.getUserDatabase

fun MainViewController() = ComposeUIViewController {
    val dao = remember {
        getUserDatabase().userDao()
    }
    App(dao)
}