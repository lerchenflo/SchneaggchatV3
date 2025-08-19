package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.lerchenflo.schneaggchatv3mp.database.userdatabase.MessageDao
import org.lerchenflo.schneaggchatv3mp.database.userdatabase.MessageReader
import kotlin.reflect.KClass


@Database(
    entities = [User::class, Message::class, MessageReader::class],
    exportSchema = true,
    version = 1
)

@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun messageDao(): MessageDao

    companion object {
        const val DB_NAME = "database.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>{
    override fun initialize(): AppDatabase
}