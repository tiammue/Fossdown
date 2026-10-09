package com.fossdown.domain

import com.fossdown.data.DisplayUnit
import com.fossdown.data.EventMode
import com.fossdown.data.ProgressEvent
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ProgressSnapshot(
    val primaryLabel: String,
    val secondaryLabel: String,
    /** All configured remaining/elapsed labels, joined for widgets. */
    val remainingLabels: List<String>,
    val percent: Float,
    val progressFraction: Float,
    val isComplete: Boolean,
    val remainingMillis: Long,
    val elapsedMillis: Long,
    val totalMillis: Long
) {
    val remainingText: String
        get() = remainingLabels.joinToString("\n")
}

object ProgressCalculator {
    const val MAX_REMAINING_LINES = 3

    fun snapshot(event: ProgressEvent, now: Long = System.currentTimeMillis()): ProgressSnapshot {
        val start = event.startEpochMillis
        val end = resolveEnd(event)
        val total = max(1L, end - start)
        val elapsed = max(0L, now - start)
        val remaining = max(0L, end - now)
        val rawFraction = when (event.mode) {
            EventMode.COUNT_UP -> {
                if (event.endEpochMillis == null) {
                    // Open-ended count-up: show elapsed growth capped visually at 1 year.
                    min(1f, elapsed.toFloat() / TimeUnit.DAYS.toMillis(365).toFloat())
                } else {
                    min(1f, elapsed.toFloat() / total.toFloat())
                }
            }
            EventMode.COUNTDOWN, EventMode.TIMER -> {
                min(1f, max(0f, elapsed.toFloat() / total.toFloat()))
            }
        }
        val isComplete = when (event.mode) {
            EventMode.COUNT_UP -> event.endEpochMillis != null && now >= end
            else -> now >= end
        }
        val percent = (rawFraction * 100f).coerceIn(0f, 100f)
        val labels = remainingLabels(event, remaining, elapsed, isComplete, percent)
        val primary = labels.firstOrNull() ?: "Complete"
        val secondary = when {
            isComplete && event.mode != EventMode.COUNT_UP -> "Complete"
            labels.size > 1 -> labels.drop(1).joinToString(" · ")
            event.displayUnit == DisplayUnit.PERCENT -> formatUnit(
                DisplayUnit.DAYS, event, remaining, elapsed, isComplete, percent
            )
            else -> "${percent.roundToInt()}%"
        }
        return ProgressSnapshot(
            primaryLabel = primary,
            secondaryLabel = secondary,
            remainingLabels = labels,
            percent = percent,
            progressFraction = rawFraction,
            isComplete = isComplete,
            remainingMillis = remaining,
            elapsedMillis = elapsed,
            totalMillis = total
        )
    }

    fun remainingLabels(
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean,
        percent: Float
    ): List<String> {
        if (complete && event.mode != EventMode.COUNT_UP) return listOf("Complete")
        val units = linkedSetOf(event.displayUnit).apply {
            addAll(event.remainingUnits)
        }.take(MAX_REMAINING_LINES)
        return units.map { unit ->
            formatUnit(unit, event, remaining, elapsed, complete, percent)
        }
    }

    private fun resolveEnd(event: ProgressEvent): Long {
        return when (event.mode) {
            EventMode.COUNT_UP -> event.endEpochMillis
                ?: (event.startEpochMillis + TimeUnit.DAYS.toMillis(365))
            EventMode.TIMER, EventMode.COUNTDOWN -> event.endEpochMillis
                ?: (event.startEpochMillis + TimeUnit.DAYS.toMillis(1))
        }
    }

    private fun formatUnit(
        unit: DisplayUnit,
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean,
        percent: Float
    ): String {
        return when (unit) {
            DisplayUnit.PERCENT -> "${percent.roundToInt()}%"
            DisplayUnit.HOURS -> formatHours(event, remaining, elapsed, complete)
            DisplayUnit.DAYS_HOURS -> formatDaysHours(event, remaining, elapsed, complete)
            DisplayUnit.WEEKS -> formatWeeks(event, remaining, elapsed, complete)
            DisplayUnit.DAYS -> formatDays(event, remaining, elapsed, complete)
        }
    }

    private fun formatDays(
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean
    ): String {
        if (complete && event.mode != EventMode.COUNT_UP) return "Complete"
        val millis = if (event.mode == EventMode.COUNT_UP) elapsed else remaining
        val days = ceil(millis / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(0)
        return when (event.mode) {
            EventMode.COUNT_UP -> if (days == 1) "1 day since" else "$days days since"
            else -> if (days == 1) "1 day left" else "$days days left"
        }
    }

    private fun formatWeeks(
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean
    ): String {
        if (complete && event.mode != EventMode.COUNT_UP) return "Complete"
        val millis = if (event.mode == EventMode.COUNT_UP) elapsed else remaining
        val weekMillis = TimeUnit.DAYS.toMillis(7).toDouble()
        val suffix = if (event.mode == EventMode.COUNT_UP) "since" else "left"
        if (event.weeksOneDecimal) {
            val weeks = (millis / weekMillis).coerceAtLeast(0.0)
            val formatted = String.format(Locale.US, "%.1f", weeks)
            val noun = if (formatted == "1.0") "week" else "weeks"
            return "$formatted $noun $suffix"
        }
        val weeks = ceil(millis / weekMillis).toInt().coerceAtLeast(0)
        return when {
            weeks == 1 -> "1 week $suffix"
            else -> "$weeks weeks $suffix"
        }
    }

    private fun formatHours(
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean
    ): String {
        if (complete && event.mode != EventMode.COUNT_UP) return "Complete"
        val millis = if (event.mode == EventMode.COUNT_UP) elapsed else remaining
        val hours = floor(millis / TimeUnit.HOURS.toMillis(1).toDouble()).toInt().coerceAtLeast(0)
        return when (event.mode) {
            EventMode.COUNT_UP -> if (hours == 1) "1 hour since" else "$hours hours since"
            else -> if (hours == 1) "1 hour left" else "$hours hours left"
        }
    }

    private fun formatDaysHours(
        event: ProgressEvent,
        remaining: Long,
        elapsed: Long,
        complete: Boolean
    ): String {
        if (complete && event.mode != EventMode.COUNT_UP) return "Complete"
        val millis = if (event.mode == EventMode.COUNT_UP) elapsed else remaining
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val suffix = if (event.mode == EventMode.COUNT_UP) "since" else "left"
        return "${days}d ${hours}h $suffix"
    }
}
