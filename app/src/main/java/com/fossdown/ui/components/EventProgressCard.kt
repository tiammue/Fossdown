package com.fossdown.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossdown.data.ProgressEvent
import com.fossdown.data.ProgressStyle
import com.fossdown.data.ThemeStyle
import com.fossdown.domain.DotGrid
import com.fossdown.domain.ProgressCalculator
import com.fossdown.domain.ProgressSnapshot
import kotlin.math.min

fun iconForKey(key: String): ImageVector = when (key) {
    "calendar" -> Icons.Outlined.CalendarMonth
    "gift" -> Icons.Outlined.CardGiftcard
    "plane" -> Icons.Outlined.Flight
    "star" -> Icons.Outlined.StarOutline
    else -> Icons.Outlined.Flag
}

@Composable
fun EventProgressCard(
    event: ProgressEvent,
    modifier: Modifier = Modifier,
    snapshot: ProgressSnapshot = ProgressCalculator.snapshot(event)
) {
    val bg = Color(event.backgroundColor)
    val accent = Color(event.accentColor)
    val text = Color(event.textColor)
    val shape = when (event.theme) {
        ThemeStyle.RETRO -> RoundedCornerShape(4.dp)
        ThemeStyle.SWISS -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(22.dp)
    }
    val dots = event.progressStyle == ProgressStyle.DOTS

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when (event.theme) {
                    ThemeStyle.AQUA -> Brush.linearGradient(listOf(bg, accent.copy(alpha = 0.35f), bg))
                    else -> Brush.verticalGradient(listOf(bg, bg))
                }
            )
            .padding(18.dp)
    ) {
        when (event.theme) {
            ThemeStyle.GRID -> GridBackdrop(accent.copy(alpha = 0.18f))
            ThemeStyle.RETRO -> RetroChrome()
            else -> Unit
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dots) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (event.showIcon) {
                        Icon(
                            imageVector = iconForKey(event.iconKey),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = event.title,
                        color = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        snapshot.remainingLabels.forEachIndexed { index, label ->
                            Text(
                                text = label,
                                color = text.copy(alpha = if (index == 0) 0.72f else 0.55f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (event.showIcon) {
                        Icon(
                            imageVector = iconForKey(event.iconKey),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = event.title,
                        color = text.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = if (event.theme == ThemeStyle.RETRO) FontFamily.Monospace else FontFamily.SansSerif
                    )
                }
                Text(
                    text = snapshot.primaryLabel,
                    color = text,
                    fontSize = when (event.theme) {
                        ThemeStyle.SWISS -> 30.sp
                        ThemeStyle.MINIMAL -> 26.sp
                        else -> 24.sp
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (event.theme) {
                        ThemeStyle.SWISS -> FontFamily.Serif
                        ThemeStyle.RETRO -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
                snapshot.remainingLabels.drop(1).forEach { label ->
                    Text(
                        text = label,
                        color = text.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (snapshot.remainingLabels.size <= 1) {
                    Text(
                        text = snapshot.secondaryLabel,
                        color = text.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            ProgressVisual(
                event = event,
                snapshot = snapshot,
                fraction = snapshot.progressFraction,
                accent = accent,
                theme = event.theme,
                style = event.progressStyle,
                rounded = event.barRounded,
                modifier = Modifier.padding(top = if (dots) 14.dp else 10.dp)
            )
        }
    }
}

@Composable
private fun ProgressVisual(
    event: ProgressEvent,
    snapshot: ProgressSnapshot,
    fraction: Float,
    accent: Color,
    theme: ThemeStyle,
    style: ProgressStyle,
    rounded: Boolean,
    modifier: Modifier = Modifier
) {
    when (style) {
        ProgressStyle.BAR -> ProgressBarVisual(fraction, accent, theme, rounded, modifier)
        ProgressStyle.DOTS -> ProgressDotsVisual(event, snapshot, accent, modifier)
        ProgressStyle.CIRCLE -> ProgressCircleVisual(fraction, accent, rounded, modifier)
        ProgressStyle.SEGMENTS -> ProgressSegmentsVisual(fraction, accent, rounded, modifier)
    }
}

@Composable
private fun ProgressBarVisual(
    fraction: Float,
    accent: Color,
    theme: ThemeStyle,
    rounded: Boolean,
    modifier: Modifier = Modifier
) {
    val track = accent.copy(alpha = 0.2f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(if (theme == ThemeStyle.MINIMAL) 10.dp else 12.dp)
    ) {
        val radius = if (rounded && theme != ThemeStyle.SWISS) size.height / 2f else 0f
        drawRoundRect(color = track, cornerRadius = CornerRadius(radius, radius))
        val barWidth = size.width * fraction.coerceIn(0f, 1f)
        if (barWidth > 0f) {
            when (theme) {
                ThemeStyle.AQUA -> drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.5f), accent)),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
                ThemeStyle.GRID -> {
                    val step = 10.dp.toPx()
                    var x = 0f
                    while (x < barWidth) {
                        drawRect(
                            color = accent,
                            topLeft = Offset(x, 0f),
                            size = Size(min(6.dp.toPx(), barWidth - x), size.height)
                        )
                        x += step
                    }
                }
                else -> drawRoundRect(
                    color = accent,
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

@Composable
private fun ProgressDotsVisual(
    event: ProgressEvent,
    snapshot: ProgressSnapshot,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val track = accent.copy(alpha = 0.28f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        val spec = DotGrid.buildSpec(
            event = event,
            snapshot = snapshot,
            widthPx = size.width,
            heightPx = size.height,
            density = density
        )
        val radius = spec.dotPx / 2f
        for (cell in spec.cells) {
            val center = Offset(
                cell.col * spec.cellWidthPx + spec.cellWidthPx / 2f,
                cell.row * spec.cellHeightPx + spec.cellHeightPx / 2f
            )
            drawCircle(color = track, radius = radius, center = center)
            val amount = cell.fill.coerceIn(0f, 1f)
            when {
                amount >= 0.999f -> drawCircle(color = accent, radius = radius, center = center)
                amount > 0.001f -> clipRect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x - radius + radius * 2f * amount,
                    bottom = center.y + radius
                ) {
                    drawCircle(color = accent, radius = radius, center = center)
                }
            }
        }
    }
}

@Composable
private fun ProgressCircleVisual(
    fraction: Float,
    accent: Color,
    rounded: Boolean,
    modifier: Modifier = Modifier
) {
    val track = accent.copy(alpha = 0.2f)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val strokeWidth = 6.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            val cap = if (rounded) StrokeCap.Round else StrokeCap.Butt
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = cap)
            )
            val sweep = 360f * fraction.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = cap)
                )
            }
        }
    }
}

@Composable
private fun ProgressSegmentsVisual(
    fraction: Float,
    accent: Color,
    rounded: Boolean,
    modifier: Modifier = Modifier
) {
    val track = accent.copy(alpha = 0.2f)
    val filled = (fraction.coerceIn(0f, 1f) * SEGMENT_COUNT)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val gap = 4.dp.toPx()
        val segmentWidth = (size.width - gap * (SEGMENT_COUNT - 1)) / SEGMENT_COUNT
        val radius = if (rounded) size.height / 2f else 0f
        for (i in 0 until SEGMENT_COUNT) {
            val left = i * (segmentWidth + gap)
            val fill = (filled - i).coerceIn(0f, 1f)
            drawRoundRect(
                color = track,
                topLeft = Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
            if (fill > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(left, 0f),
                    size = Size(segmentWidth * fill, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

private const val SEGMENT_COUNT = 8

@Composable
private fun BoxScope.GridBackdrop(color: Color) {
    Canvas(modifier = Modifier.matchParentSize()) {
        val step = 18.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

@Composable
private fun BoxScope.RetroChrome() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopEnd),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF5F56))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFBD2E))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF27C93F))
        )
    }
    Canvas(modifier = Modifier.matchParentSize()) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            ),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}
