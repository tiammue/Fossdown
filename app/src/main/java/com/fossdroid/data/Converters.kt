package com.fossdroid.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toEventMode(value: String): EventMode = EventMode.valueOf(value)

    @TypeConverter
    fun fromEventMode(value: EventMode): String = value.name

    @TypeConverter
    fun toDisplayUnit(value: String): DisplayUnit = DisplayUnit.valueOf(value)

    @TypeConverter
    fun fromDisplayUnit(value: DisplayUnit): String = value.name

    @TypeConverter
    fun toDisplayUnitList(value: String): List<DisplayUnit> {
        if (value.isBlank()) return emptyList()
        return value.split(',')
            .mapNotNull { token ->
                runCatching { DisplayUnit.valueOf(token.trim()) }.getOrNull()
            }
            .distinct()
    }

    @TypeConverter
    fun fromDisplayUnitList(value: List<DisplayUnit>): String {
        return value.distinct().joinToString(",") { it.name }
    }

    @TypeConverter
    fun toProgressStyle(value: String): ProgressStyle = ProgressStyle.valueOf(value)

    @TypeConverter
    fun fromProgressStyle(value: ProgressStyle): String = value.name

    @TypeConverter
    fun toWeekDotMode(value: String): WeekDotMode = when (value) {
        "EXPAND_DAYS" -> WeekDotMode.PARTIAL // removed mode; treat as partial
        else -> WeekDotMode.valueOf(value)
    }

    @TypeConverter
    fun fromWeekDotMode(value: WeekDotMode): String = value.name

    @TypeConverter
    fun toThemeStyle(value: String): ThemeStyle = ThemeStyle.valueOf(value)

    @TypeConverter
    fun fromThemeStyle(value: ThemeStyle): String = value.name
}
