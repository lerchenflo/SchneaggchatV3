package org.lerchenflo.schneaggchatv3mp.database.userdatabase

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.lerchenflo.schneaggchatv3mp.database.messagedatabase.UserDatabase

class CreateUserDatabase(private val builder: RoomDatabase.Builder<UserDatabase>) {

    fun getDatabase(): UserDatabase {
        return builder
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}