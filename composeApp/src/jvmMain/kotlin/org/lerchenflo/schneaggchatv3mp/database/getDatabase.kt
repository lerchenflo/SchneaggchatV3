package org.lerchenflo.schneaggchatv3mp.database

import androidx.compose.ui.text.toLowerCase
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.util.Locale

fun getUserDatabase(): UserDatabase {
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
    val dbFile = File(appDataDir, UserDatabase.DB_NAME)
    return Room.databaseBuilder<UserDatabase>(dbFile.absolutePath)
        .build()
}