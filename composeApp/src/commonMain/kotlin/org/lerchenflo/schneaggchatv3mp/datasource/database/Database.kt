package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupMemberEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageReaderEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.SnailTrailPointEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.UserEntity
import org.lerchenflo.schneaggchatv3mp.events.data.local.entity.EventEntity
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.entity.MapEntryEntity


@Database(
    entities = [UserEntity::class, MessageEntity::class, MessageReaderEntity::class, GroupEntity::class, GroupMemberEntity::class, LogEntry::class, PlayerEntity::class, MapEntryEntity::class, SnailTrailPointEntity::class, EventEntity::class],
    exportSchema = true,
    version = 87
)

@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun messageDao(): MessageDao

    abstract fun messageReaderDao(): MessageReaderDao

    abstract fun groupDao(): GroupDao

    abstract fun logDao(): LogDao

    abstract fun playerDao(): PlayerDao

    abstract fun mapEntryDao(): MapEntryDao

    abstract fun eventDao(): EventDao

    abstract fun snailTrailDao(): SnailTrailDao

    abstract fun allDatabaseDao(): AllDatabaseDao


    companion object {
        const val DB_NAME = "database.db"
    }
}


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>{
    override fun initialize(): AppDatabase
}