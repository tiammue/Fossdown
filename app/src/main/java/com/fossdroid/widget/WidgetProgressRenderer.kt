package com.fossdroid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.fossdroid.data.ProgressEvent
import com.fossdroid.data.ProgressStyle
import com.fossdroid.domain.DotGrid
import com.fossdroid.domain.DotGridSpec
import com.fossdroid.domain.ProgressSnapshot
import kotlin.math.min

object WidgetProgressRenderer {
    private const val SEGMENT_COUNT = 10

    fun render(
        context: Context,
        style: ProgressStyle,
        fraction: Float,
        accentColor: Int,
        textColor: Int,
        rounded: Boolean,
        widthDp: Float,
        heightDp: Float,
        event: ProgressEvent? = null,
        snapshot: ProgressSnapshot? = null,
        centerLabel: String = ""
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val progress = fraction.coerceIn(0f, 1f)
        val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(1)
        val trackColor = withAlpha(accentColor, 0.28f)
        return when (style) {
            ProgressStyle.CIRCLE -> drawCircle(
                widthPx, heightPx, progress, accentColor, trackColor, textColor, rounded, centerLabel
            )
            ProgressStyle.DOTS -> {
                val spec = if (event != null && snapshot != null) {
                    DotGrid.buildSpec(event, snapshot, widthPx.toFloat(), heightPx.toFloat(), density)
                } else {
                    DotGridSpec(
                        columns = 1,
                        rows = 1,
                        cells = emptyList(),
                        cellWidthPx = widthPx.toFloat(),
                        cellHeightPx = heightPx.toFloat(),
                        dotPx = 0f
                    )
                }
                drawDotGrid(widthPx, heightPx, spec, accentColor, trackColor)
            }
            ProgressStyle.SEGMENTS -> drawSegments(
                widthPx, heightPx, progress, accentColor, trackColor, rounded
            )
            ProgressStyle.BAR -> drawBar(
                widthPx, heightPx, progress, accentColor, trackColor, rounded
            )
        }
    }

    private fun drawBar(
        width: Int,
        height: Int,
        progress: Float,
        accent: Int,
        track: Int,
        rounded: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val barHeight = min(height * 0.22f, 18f * (width / 360f)).coerceIn(10f, 28f)
        val top = (height - barHeight) / 2f
        val radius = if (rounded) barHeight / 2f else 0f
        val bounds = RectF(0f, top, width.toFloat(), top + barHeight)
        paint.color = track
        canvas.drawRoundRect(bounds, radius, radius, paint)
        val filled = width * progress
        if (filled > 0f) {
            paint.color = accent
            canvas.drawRoundRect(RectF(0f, top, filled, top + barHeight), radius, radius, paint)
        }
        return bitmap
    }

    fun drawDotGrid(
        canvasWidth: Int,
        canvasHeight: Int,
        spec: DotGridSpec,
        accent: Int,
        track: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = spec.dotPx / 2f
        for (cell in spec.cells) {
            val cx = cell.col * spec.cellWidthPx + spec.cellWidthPx / 2f
            val cy = cell.row * spec.cellHeightPx + spec.cellHeightPx / 2f
            paint.color = track
            canvas.drawCircle(cx, cy, radius, paint)
            val amount = cell.fill.coerceIn(0f, 1f)
            if (amount >= 0.999f) {
                paint.color = accent
                canvas.drawCircle(cx, cy, radius, paint)
            } else if (amount > 0.001f) {
                canvas.save()
                canvas.clipRect(
                    cx - radius,
                    cy - radius,
                    cx - radius + radius * 2f * amount,
                    cy + radius
                )
                paint.color = accent
                canvas.drawCircle(cx, cy, radius, paint)
                canvas.restore()
            }
        }
        return bitmap
    }

    private fun drawCircle(
        width: Int,
        height: Int,
        progress: Float,
        accent: Int,
        track: Int,
        textColor: Int,
        rounded: Boolean,
        centerLabel: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val size = min(width, height).toFloat()
        val stroke = size * 0.09f
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - stroke
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT
        }
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.color = track
        canvas.drawArc(oval, -90f, 360f, false, paint)
        if (progress > 0f) {
            paint.color = accent
            canvas.drawArc(oval, -90f, 360f * progress, false, paint)
        }
        if (centerLabel.isNotBlank()) {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = size * 0.28f
            }
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(centerLabel, cx, textY, textPaint)
        }
        return bitmap
    }

    private fun drawSegments(
        width: Int,
        height: Int,
        progress: Float,
        accent: Int,
        track: Int,
        rounded: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val barHeight = min(height * 0.22f, 18f * (width / 360f)).coerceIn(10f, 28f)
        val top = (height - barHeight) / 2f
        val gap = barHeight * 0.35f
        val segmentWidth = (width - gap * (SEGMENT_COUNT - 1)) / SEGMENT_COUNT
        val radius = if (rounded) barHeight / 2f else 0f
        val filled = progress * SEGMENT_COUNT
        for (i in 0 until SEGMENT_COUNT) {
            val left = i * (segmentWidth + gap)
            val fill = (filled - i).coerceIn(0f, 1f)
            paint.color = track
            canvas.drawRoundRect(
                RectF(left, top, left + segmentWidth, top + barHeight),
                radius,
                radius,
                paint
            )
            if (fill > 0f) {
                paint.color = accent
                canvas.drawRoundRect(
                    RectF(left, top, left + segmentWidth * fill, top + barHeight),
                    radius,
                    radius,
                    paint
                )
            }
        }
        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}
