package org.lerchenflo.schneaggchatv3mp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.OldServerUrlMigration

fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        migrations = listOf(OldServerUrlMigration),
        produceFile = {producePath().toPath()}
    )
}




internal const val DATA_STORE_FILE_NAME = "prefs.preferences_pb"