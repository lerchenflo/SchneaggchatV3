package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.lerchenflo.schneaggchatv3mp.database.tables.Group
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupMember
import org.lerchenflo.schneaggchatv3mp.database.tables.Message
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageReader
import org.lerchenflo.schneaggchatv3mp.database.tables.User


@Database(
    entities = [User::class, Message::class, MessageReader::class, Group::class, GroupMember::class],
    exportSchema = true,
    version = 20
)

@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun messageDao(): MessageDao

    abstract fun messagereaderDao(): MessageReaderDao

    abstract fun groupDao(): GroupDao


    abstract fun allDatabaseDao(): AllDatabaseDao


    companion object {
        const val DB_NAME = "database.db"
    }
}


expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>{
    override fun initialize(): AppDatabase
}