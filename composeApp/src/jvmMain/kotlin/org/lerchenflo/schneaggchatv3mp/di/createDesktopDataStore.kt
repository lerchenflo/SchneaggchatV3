package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun desktopDatastoreBuilder(): DataStore<Preferences> {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val appDataDir = when {
        os.contains("win") -> File(System.getenv("APPDATA"), "Schneaggchat")
        os.contains("mac") -> File(userHome, "Library/Application Support/Schneaggchat")
        else -> File(userHome, ".local/share/Schneaggchat")
    }

    if (!appDataDir.exists()){
        appDataDir.mkdirs()
    }
    val savefile = File(appDataDir, DATA_STORE_FILE_NAME)

    return createDataStore {
        savefile.absolutePath
    }
}