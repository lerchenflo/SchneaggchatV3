package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import kotlin.reflect.KClass


@Database(
    entities = [User::class],
    exportSchema = true,
    version = 1
)

@ConstructedBy(AppDatabaseConstructor::class)
abstract class UserDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val DB_NAME = "users.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<UserDatabase>{
    override fun initialize(): UserDatabase
}