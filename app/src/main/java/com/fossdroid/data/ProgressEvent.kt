package com.fossdroid.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventMode {
    COUNTDOWN,
    COUNT_UP,
    TIMER
}

enum class DisplayUnit {
    DAYS,
    WEEKS,
    HOURS,
    PERCENT,
    DAYS_HOURS
}

enum class ProgressStyle {
    BAR,
    DOTS,
    CIRCLE,
    SEGMENTS
}

/** How the in-progress week is drawn when [DisplayUnit.WEEKS] + dots. */
enum class WeekDotMode {
    /** Current week stays empty until the whole week has passed. */
    GREY_UNTIL_DONE,
    /** Current week is partially filled by days elapsed in that week. */
    PARTIAL
}

enum class ThemeStyle {
    SWISS,
    MINIMAL,
    AQUA,
    GRID,
    RETRO
}

@Entity(tableName = "events")
data class ProgressEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mode: EventMode = EventMode.COUNTDOWN,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val allDay: Boolean = true,
    val theme: ThemeStyle = ThemeStyle.MINIMAL,
    val accentColor: Long = 0xFF7CFFB2,
    val backgroundColor: Long = 0xFF121212,
    val textColor: Long = 0xFFFFFFFF,
    val displayUnit: DisplayUnit = DisplayUnit.DAYS,
    /** Extra remaining labels shown with the primary [displayUnit] text. */
    val remainingUnits: List<DisplayUnit> = emptyList(),
    /** When true, weeks remaining/since use one decimal place (e.g. 3.2). */
    val weeksOneDecimal: Boolean = false,
    val showIcon: Boolean = true,
    val iconKey: String = "flag",
    val progressStyle: ProgressStyle = ProgressStyle.BAR,
    val weekDotMode: WeekDotMode = WeekDotMode.PARTIAL,
    val barRounded: Boolean = true,
    val notes: String = "",
    val archived: Boolean = false,
    val notifyDayBefore: Boolean = true,
    val notifyOnComplete: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
