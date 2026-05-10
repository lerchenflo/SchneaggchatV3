package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction

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

    @TypeConverter
    fun fromReactionList(reactions: List<Reaction>): String {
        return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Reaction.serializer()), reactions)
    }

    @TypeConverter
    fun toReactionList(reactionsString: String): List<Reaction> {
        return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Reaction.serializer()), reactionsString)
    }

}