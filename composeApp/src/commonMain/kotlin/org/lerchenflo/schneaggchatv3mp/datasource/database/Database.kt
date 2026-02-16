package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupMemberDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoEntityDto
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity


@Database(
    entities = [UserDto::class, MessageDto::class, MessageReaderDto::class, GroupDto::class, GroupMemberDto::class, TodoEntityDto::class, LogEntry::class, PlayerEntity::class],
    exportSchema = true,
    version = 51
)

@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun messageDao(): MessageDao

    abstract fun messageReaderDao(): MessageReaderDao

    abstract fun groupDao(): GroupDao

    abstract fun todoListDao(): TodolistDao

    abstract fun logDao(): LogDao

    abstract fun playerDao(): PlayerDao

    abstract fun allDatabaseDao(): AllDatabaseDao


    companion object {
        const val DB_NAME = "database.db"
    }
}


expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>{
    override fun initialize(): AppDatabase
}