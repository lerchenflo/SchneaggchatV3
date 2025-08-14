package org.lerchenflo.schneaggchatv3mp.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun getUserDatabase(context: Context): UserDatabase{
    val dbFile = context.getDatabasePath(UserDatabase.DB_NAME)
    return Room.databaseBuilder<UserDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}