package com.fossdroid

import com.fossdroid.data.DisplayUnit
import com.fossdroid.data.EventMode
import com.fossdroid.data.ProgressEvent
import com.fossdroid.domain.ProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProgressCalculatorTest {
    @Test
    fun countdownShowsDaysLeftAndPercent() {
        val start = 1_000_000L
        val end = start + TimeUnit.DAYS.toMillis(10)
        val now = start + TimeUnit.DAYS.toMillis(4)
        val event = ProgressEvent(
            title = "Trip",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.DAYS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("6 days left", snap.primaryLabel)
        assertEquals(40, snap.percent.toInt())
        assertFalse(snap.isComplete)
    }

    @Test
    fun countdownCompletesAtEnd() {
        val start = 1_000_000L
        val end = start + TimeUnit.DAYS.toMillis(5)
        val event = ProgressEvent(
            title = "Deadline",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end
        )
        val snap = ProgressCalculator.snapshot(event, end)
        assertTrue(snap.isComplete)
        assertEquals("Complete", snap.primaryLabel)
        assertEquals(100f, snap.percent, 0.01f)
    }

    @Test
    fun countUpShowsDaysSince() {
        val start = 1_000_000L
        val now = start + TimeUnit.DAYS.toMillis(3)
        val event = ProgressEvent(
            title = "Habit",
            mode = EventMode.COUNT_UP,
            startEpochMillis = start,
            endEpochMillis = null,
            displayUnit = DisplayUnit.DAYS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("3 days since", snap.primaryLabel)
        assertFalse(snap.isComplete)
    }

    @Test
    fun percentDisplayUnit() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(100)
        val now = TimeUnit.DAYS.toMillis(25)
        val event = ProgressEvent(
            title = "Year",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.PERCENT
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("25%", snap.primaryLabel)
    }

    @Test
    fun daysHoursFormat() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(5)
        val now = 0L
        val event = ProgressEvent(
            title = "Launch",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.DAYS_HOURS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("3d 5h left", snap.primaryLabel)
    }

    @Test
    fun weeksFormat() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(21)
        val now = TimeUnit.DAYS.toMillis(7)
        val event = ProgressEvent(
            title = "Sprint",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.WEEKS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("2 weeks left", snap.primaryLabel)
    }

    @Test
    fun weeksCountUpFormat() {
        val start = 0L
        val now = TimeUnit.DAYS.toMillis(7)
        val event = ProgressEvent(
            title = "Habit",
            mode = EventMode.COUNT_UP,
            startEpochMillis = start,
            endEpochMillis = null,
            displayUnit = DisplayUnit.WEEKS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("1 week since", snap.primaryLabel)
    }

    @Test
    fun weeksOneDecimalFormat() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(24)
        val now = TimeUnit.DAYS.toMillis(10)
        val event = ProgressEvent(
            title = "Sprint",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.WEEKS,
            weeksOneDecimal = true
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals("2.0 weeks left", snap.primaryLabel)
    }

    @Test
    fun multipleRemainingLabels() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(14)
        val now = TimeUnit.DAYS.toMillis(7)
        val event = ProgressEvent(
            title = "Trip",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.WEEKS,
            remainingUnits = listOf(DisplayUnit.DAYS, DisplayUnit.HOURS),
            weeksOneDecimal = true
        )
        val snap = ProgressCalculator.snapshot(event, now)
        assertEquals(
            listOf("1.0 week left", "7 days left", "168 hours left"),
            snap.remainingLabels
        )
        assertEquals("1.0 week left\n7 days left\n168 hours left", snap.remainingText)
    }
}
