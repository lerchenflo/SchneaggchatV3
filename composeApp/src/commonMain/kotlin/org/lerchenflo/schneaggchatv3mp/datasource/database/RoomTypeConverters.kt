package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
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
        return pollMessageString?.let { json.decodeFromString(PollMessage.serializer(), it) }
    }


    @TypeConverter
    fun locationDataToString(data: LocationData): String =
        json.encodeToString(LocationData.serializer(), data)

    @TypeConverter
    fun stringToLocationData(value: String): LocationData =
        json.decodeFromString(LocationData.serializer(), value)

    @TypeConverter
    fun latLongToString(latLong: LatLong): String =
        json.encodeToString(LatLong.serializer(), latLong)

    @TypeConverter
    fun stringToLatLong(value: String): LatLong =
        json.decodeFromString(LatLong.serializer(), value)
}