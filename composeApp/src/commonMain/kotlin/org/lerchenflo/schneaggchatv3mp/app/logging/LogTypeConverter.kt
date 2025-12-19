package org.lerchenflo.schneaggchatv3mp.app.logging

import androidx.room.TypeConverter

class LogTypeConverter {
    @TypeConverter
    fun fromLogType(value: LogType): String {
        return value.name
    }

    @TypeConverter
    fun toLogType(value: String): LogType {
        return LogType.valueOf(value)
    }
}
