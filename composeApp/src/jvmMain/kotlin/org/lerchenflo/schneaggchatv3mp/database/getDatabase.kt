package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import java.io.File
import java.lang.System


fun desktopAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
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
    val dbFile = File(appDataDir, AppDatabase.DB_NAME)
    return Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
}

