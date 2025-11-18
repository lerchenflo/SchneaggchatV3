package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun iosAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {

    val documentDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )



    val dbFile = documentDir?.path + "/" + AppDatabase.DB_NAME

    return Room.databaseBuilder<AppDatabase>(
        name = dbFile
    )
}