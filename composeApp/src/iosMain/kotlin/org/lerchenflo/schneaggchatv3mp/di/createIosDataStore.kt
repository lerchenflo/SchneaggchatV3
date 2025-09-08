package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun iosDatastoreBuilder(): DataStore<Preferences> {
    val documentDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    val dbFile = documentDir?.path + "/" + DATA_STORE_FILE_NAME

    return createDataStore {
        dbFile
    }
}