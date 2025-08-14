package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSHomeDirectory

fun getUserDatabase(): UserDatabase {
    val dbFile = NSHomeDirectory() + "/users.db"
    return Room.databaseBuilder<UserDatabase>(
        name = dbFile,
        factory = { UserDatabase::class.instantiateImpl() }
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}