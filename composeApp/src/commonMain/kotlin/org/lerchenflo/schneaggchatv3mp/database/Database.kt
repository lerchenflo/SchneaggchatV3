package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor


@Database(
    entities = [User::class, Message::class, MessageReader::class],
    exportSchema = true,
    version = 12
)

@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun messageDao(): MessageDao

    abstract fun messagereaderDao(): MessageReaderDao
    abstract fun allDatabaseDao(): AllDatabaseDao

    companion object {
        const val DB_NAME = "database.db"
    }
}


expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>{
    override fun initialize(): AppDatabase
}