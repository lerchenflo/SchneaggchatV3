package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage

class RoomTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromPollMessage(pollMessage: PollMessage?): String? {
        return pollMessage?.let { json.encodeToString(PollMessage.serializer(), it) }
    }

    @TypeConverter
    fun toPollMessage(pollMessageString: String?): PollMessage? {
        return pollMessageString?.let { json.decodeFromString(PollMessage.serializer(), it) }
    }

}