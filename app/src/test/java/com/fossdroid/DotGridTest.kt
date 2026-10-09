package com.fossdroid

import com.fossdroid.data.DisplayUnit
import com.fossdroid.data.EventMode
import com.fossdroid.data.ProgressEvent
import com.fossdroid.data.WeekDotMode
import com.fossdroid.domain.DotGrid
import com.fossdroid.domain.ProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DotGridTest {
    @Test
    fun countdownFillsElapsedDays() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(100)
        val now = TimeUnit.DAYS.toMillis(25)
        val event = ProgressEvent(
            title = "Trip",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.DAYS
        )
        val snap = ProgressCalculator.snapshot(event, now)
        val fills = DotGrid.buildFills(event, snap)
        assertEquals(100, fills.size)
        assertEquals(25, fills.count { it >= 1f })
    }

    @Test
    fun weeksGreyUntilDoneIgnoresPartialWeek() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(98)
        val now = TimeUnit.DAYS.toMillis(24)
        val event = ProgressEvent(
            title = "Home",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.WEEKS,
            weekDotMode = WeekDotMode.GREY_UNTIL_DONE
        )
        val snap = ProgressCalculator.snapshot(event, now)
        val fills = DotGrid.buildFills(event, snap)
        assertEquals(14, fills.size)
        assertEquals(3, fills.count { it >= 1f })
        assertTrue(fills.drop(3).all { it == 0f })
    }

    @Test
    fun weeksPartialFillsCurrentWeek() {
        val start = 0L
        val end = TimeUnit.DAYS.toMillis(98)
        val now = TimeUnit.DAYS.toMillis(24)
        val event = ProgressEvent(
            title = "Home",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = start,
            endEpochMillis = end,
            displayUnit = DisplayUnit.WEEKS,
            weekDotMode = WeekDotMode.PARTIAL
        )
        val snap = ProgressCalculator.snapshot(event, now)
        val fills = DotGrid.buildFills(event, snap)
        assertEquals(14, fills.size)
        assertEquals(listOf(1f, 1f, 1f), fills.take(3))
        assertEquals(3f / 7f, fills[3], 0.001f)
        assertTrue(fills.drop(4).all { it == 0f })
    }

    @Test
    fun layoutFillsAvailableHeight() {
        val density = 3f
        val width = 320f * density
        val height = 140f * density
        val event = ProgressEvent(
            title = "Year",
            mode = EventMode.COUNTDOWN,
            startEpochMillis = 0L,
            endEpochMillis = TimeUnit.DAYS.toMillis(124),
            displayUnit = DisplayUnit.DAYS
        )
        val snap = ProgressCalculator.snapshot(event, TimeUnit.DAYS.toMillis(24))
        val spec = DotGrid.buildSpec(event, snap, width, height, density)
        assertTrue(spec.columns >= 12)
        assertTrue(spec.rows >= 4)
        assertEquals(124, spec.totalDots)
        assertTrue(abs(spec.widthPx - width) < 1f)
        assertTrue(abs(spec.heightPx - height) < 1f)
    }
}
