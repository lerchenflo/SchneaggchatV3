package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import platform.Foundation.NSHomeDirectory

fun iosDatastoreBuilder(): DataStore<Preferences> {
    val dbFile = NSHomeDirectory() + "/" + DATA_STORE_FILE_NAME
    return createDataStore {
        dbFile
    }
}