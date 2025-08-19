package org.lerchenflo.schneaggchatv3mp.database.messagedatabase

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor


@Database(
    entities = [Message::class, MessageReader::class],
    version = 1,
    exportSchema = true
)

@ConstructedBy(MessageDatabaseConstructor::class)
abstract class MessageDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        const val DB_NAME = "messages.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MessageDatabaseConstructor : RoomDatabaseConstructor<MessageDatabase>{
    override fun initialize(): MessageDatabase
}