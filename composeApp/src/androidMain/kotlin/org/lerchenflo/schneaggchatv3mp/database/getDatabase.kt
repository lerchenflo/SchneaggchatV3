package org.lerchenflo.schneaggchatv3mp.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun androidUserDatabaseBuilder(context: Context): RoomDatabase.Builder<UserDatabase>{
    val dbFile = context.getDatabasePath(UserDatabase.DB_NAME)
    return Room.databaseBuilder<UserDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
