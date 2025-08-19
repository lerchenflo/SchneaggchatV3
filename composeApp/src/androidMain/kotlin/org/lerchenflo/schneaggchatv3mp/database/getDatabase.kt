package org.lerchenflo.schneaggchatv3mp.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun androidAppDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase>{
    val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
