package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventMessage
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

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
        // Tolerate a stale/malformed column instead of throwing - a poll that fails to decode
        // should render as null, not crash the message list.
        return pollMessageString?.let {
            runCatching { json.decodeFromString(PollMessage.serializer(), it) }.getOrNull()
        }
    }

    @TypeConverter
    fun fromSystemEvent(systemEvent: SystemEventMessage?): String? {
        return systemEvent?.let { json.encodeToString(SystemEventMessage.serializer(), it) }
    }

    @TypeConverter
    fun toSystemEvent(systemEventString: String?): SystemEventMessage? {
        // Same tolerance as toPollMessage - a system message that fails to decode should render
        // as nothing, not crash the message list.
        return systemEventString?.let {
            runCatching { json.decodeFromString(SystemEventMessage.serializer(), it) }.getOrNull()
        }
    }

    @TypeConverter
    fun fromReactionList(reactions: List<Reaction>): String {
        return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Reaction.serializer()), reactions)
    }

    @TypeConverter
    fun toReactionList(reactionsString: String): List<Reaction> {
        return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Reaction.serializer()), reactionsString)
    }


    @TypeConverter
    fun locationDataToString(data: LocationData): String =
        json.encodeToString(LocationData.serializer(), data)

    @TypeConverter
    fun stringToLocationData(value: String): LocationData =
        json.decodeFromString(LocationData.serializer(), value)

    @TypeConverter
    fun locationDataListToString(data: List<LocationData>): String =
        json.encodeToString(data)

    @TypeConverter
    fun stringToLocationDataList(value: String): List<LocationData> =
        json.decodeFromString(value)

    @TypeConverter
    fun latLongToString(latLong: LatLong?): String? =
        latLong?.let { json.encodeToString(LatLong.serializer(), it) }

    @TypeConverter
    fun stringToLatLong(value: String?): LatLong? =
        value?.let { json.decodeFromString(LatLong.serializer(), it) }

    @TypeConverter
    fun eventTypeToString(type: EventType): String = type.name

    @TypeConverter
    fun stringToEventType(value: String): EventType =
        runCatching { EventType.valueOf(value) }.getOrDefault(EventType.OTHER)

    @TypeConverter
    fun eventVisibilityToString(visibility: EventVisibility): String = visibility.name

    @TypeConverter
    fun stringToEventVisibility(value: String): EventVisibility =
        runCatching { EventVisibility.valueOf(value) }.getOrDefault(EventVisibility.FRIENDS_ONLY)

    @TypeConverter
    fun groupDeleteDelayToString(delay: GroupDeleteDelay): String = delay.name

    @TypeConverter
    fun stringToGroupDeleteDelay(value: String): GroupDeleteDelay =
        runCatching { GroupDeleteDelay.valueOf(value) }.getOrDefault(GroupDeleteDelay.ONE_DAY)

    @TypeConverter
    fun stringListToString(list: List<String>): String =
        json.encodeToString(list)

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}