package com.fossdown.domain

import com.fossdown.data.DisplayUnit
import com.fossdown.data.ProgressEvent
import com.fossdown.data.WeekDotMode
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DotCell(
    val fill: Float,
    val col: Int,
    val row: Int
)

/**
 * Unit-grid of day/week dots, scaled to fill the available rectangle.
 */
data class DotGridSpec(
    val columns: Int,
    val rows: Int,
    val cells: List<DotCell>,
    val cellWidthPx: Float,
    val cellHeightPx: Float,
    val dotPx: Float
) {
    val totalDots: Int get() = cells.size
    val widthPx: Float get() = columns * cellWidthPx
    val heightPx: Float get() = rows * cellHeightPx
}

object DotGrid {
    private const val DOT_FILL = 0.62f
    private const val MIN_COLUMNS = 4
    private const val MAX_COLUMNS = 28

    fun unitMillis(displayUnit: DisplayUnit): Long = when (displayUnit) {
        DisplayUnit.WEEKS -> TimeUnit.DAYS.toMillis(7)
        DisplayUnit.HOURS -> TimeUnit.HOURS.toMillis(1)
        DisplayUnit.DAYS,
        DisplayUnit.DAYS_HOURS,
        DisplayUnit.PERCENT -> TimeUnit.DAYS.toMillis(1)
    }

    fun unitCounts(event: ProgressEvent, snapshot: ProgressSnapshot): Pair<Int, Int> {
        val unitMs = unitMillis(event.displayUnit)
        val total = max(1, ceil(snapshot.totalMillis / unitMs.toDouble()).toInt())
        val elapsed = min(total, max(0, (snapshot.elapsedMillis / unitMs).toInt()))
        return total to elapsed
    }

    fun remainingUnits(event: ProgressEvent, snapshot: ProgressSnapshot): Int {
        val unitMs = unitMillis(event.displayUnit).toDouble()
        return ceil(snapshot.remainingMillis / unitMs).toInt().coerceAtLeast(0)
    }

    fun buildFills(event: ProgressEvent, snapshot: ProgressSnapshot): List<Float> {
        if (event.displayUnit != DisplayUnit.WEEKS) {
            val (total, filled) = unitCounts(event, snapshot)
            return List(total) { index -> if (index < filled) 1f else 0f }
        }
        val weekMs = TimeUnit.DAYS.toMillis(7)
        val totalWeeks = max(1, ceil(snapshot.totalMillis / weekMs.toDouble()).toInt())
        val elapsed = snapshot.elapsedMillis.coerceIn(0L, snapshot.totalMillis)
        val completeWeeks = min(totalWeeks, (elapsed / weekMs).toInt())
        val elapsedInWeek = (elapsed % weekMs).toDouble()
        return when (event.weekDotMode) {
            WeekDotMode.GREY_UNTIL_DONE -> {
                List(totalWeeks) { index -> if (index < completeWeeks) 1f else 0f }
            }
            WeekDotMode.PARTIAL -> {
                val fraction = (elapsedInWeek / weekMs).toFloat().coerceIn(0f, 1f)
                List(totalWeeks) { index ->
                    when {
                        index < completeWeeks -> 1f
                        index == completeWeeks && completeWeeks < totalWeeks -> fraction
                        else -> 0f
                    }
                }
            }
        }
    }

    fun buildSpec(
        event: ProgressEvent,
        snapshot: ProgressSnapshot,
        widthPx: Float,
        heightPx: Float?,
        density: Float
    ): DotGridSpec {
        return layout(buildFills(event, snapshot), widthPx, heightPx, density)
    }

    private fun layout(
        fills: List<Float>,
        widthPx: Float,
        heightPx: Float?,
        density: Float
    ): DotGridSpec {
        val width = widthPx.coerceAtLeast(1f)
        val normalized = fills.ifEmpty { listOf(0f) }
        val units = normalized.size
        val aspect = if (heightPx != null && heightPx > 0f) width / heightPx else 2.2f
        val columns = chooseColumns(units, aspect)
        val rows = max(1, ceil(units / columns.toDouble()).toInt())
        val cells = normalized.mapIndexed { i, fill ->
            DotCell(fill = fill, col = i % columns, row = i / columns)
        }

        if (heightPx == null || heightPx <= 0f) {
            val cell = width / columns
            return DotGridSpec(
                columns = columns,
                rows = rows,
                cells = cells,
                cellWidthPx = cell,
                cellHeightPx = cell,
                dotPx = cell * DOT_FILL
            )
        }

        val height = heightPx.coerceAtLeast(1f)
        val cellW = width / columns
        val cellH = height / rows
        val dot = min(cellW, cellH) * DOT_FILL
        return DotGridSpec(
            columns = columns,
            rows = rows,
            cells = cells,
            cellWidthPx = cellW,
            cellHeightPx = cellH,
            dotPx = dot.coerceAtLeast(2f * density)
        )
    }

    private fun chooseColumns(units: Int, aspect: Float): Int {
        val target = sqrt(units * aspect.toDouble()).roundToInt()
        return target.coerceIn(MIN_COLUMNS, min(MAX_COLUMNS, units.coerceAtLeast(1)))
    }
}
