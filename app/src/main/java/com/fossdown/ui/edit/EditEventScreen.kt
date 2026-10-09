package com.fossdown.ui.edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fossdown.data.DisplayUnit
import com.fossdown.data.EventMode
import com.fossdown.data.ProgressEvent
import com.fossdown.data.ProgressStyle
import com.fossdown.data.ThemeStyle
import com.fossdown.data.WeekDotMode
import com.fossdown.ui.components.EventProgressCard
import java.text.DateFormat
import java.util.Calendar

private val accentChoices = listOf(
    0xFF7CFFB2, 0xFF4FC3F7, 0xFFFF6B6B, 0xFFFFD166, 0xFFC792EA, 0xFFFF9F1C, 0xFF2EC4B6, 0xFFFFFFFF
)
private val backgroundChoices = listOf(
    0xFF121212, 0xFF1B2430, 0xFF0E0E0E, 0xFF1C1C1E, 0xFF102A43, 0xFF2B2118, 0xFFF7F4EF, 0xFFFFFFFF
)
private val iconChoices = listOf("flag", "calendar", "gift", "plane", "star")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    viewModel: EditEventViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id > 0) "Edit event" else "New event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.id > 0) {
                        IconButton(onClick = { viewModel.delete(onDone) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = { viewModel.save(onDone) }) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EventProgressCard(
                event = ProgressEvent(
                    id = state.id,
                    title = state.title.ifBlank { "Untitled" },
                    mode = state.mode,
                    startEpochMillis = state.startMillis,
                    endEpochMillis = if (state.mode == EventMode.COUNT_UP && !state.hasEnd) null else state.endMillis,
                    theme = state.theme,
                    accentColor = state.accentColor,
                    backgroundColor = state.backgroundColor,
                    textColor = state.textColor,
                    displayUnit = state.displayUnit,
                    remainingUnits = state.remainingUnits,
                    weeksOneDecimal = state.weeksOneDecimal,
                    showIcon = state.showIcon,
                    iconKey = state.iconKey,
                    progressStyle = state.progressStyle,
                    weekDotMode = state.weekDotMode,
                    barRounded = state.barRounded
                )
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.update { s -> s.copy(title = it) } },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Mode", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = {
                            viewModel.update { s ->
                                s.copy(
                                    mode = mode,
                                    hasEnd = mode != EventMode.COUNT_UP || s.hasEnd
                                )
                            }
                        },
                        label = {
                            Text(
                                when (mode) {
                                    EventMode.COUNTDOWN -> "Countdown"
                                    EventMode.COUNT_UP -> "Count up"
                                    EventMode.TIMER -> "Timer"
                                }
                            )
                        }
                    )
                }
            }

            DateField(
                label = "Start",
                value = if (state.allDay) dateFormat.format(state.startMillis) else dateTimeFormat.format(state.startMillis),
                onClick = {
                    pickDateTime(context, state.startMillis, state.allDay) { millis ->
                        viewModel.update { it.copy(startMillis = millis) }
                    }
                }
            )

            if (state.mode != EventMode.COUNT_UP || state.hasEnd) {
                DateField(
                    label = if (state.mode == EventMode.TIMER) "Ends after" else "End",
                    value = if (state.allDay) dateFormat.format(state.endMillis) else dateTimeFormat.format(state.endMillis),
                    onClick = {
                        pickDateTime(context, state.endMillis, state.allDay) { millis ->
                            viewModel.update { it.copy(endMillis = millis) }
                        }
                    }
                )
            }

            if (state.mode == EventMode.COUNT_UP) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open-ended count up", modifier = Modifier.weight(1f))
                    Switch(
                        checked = !state.hasEnd,
                        onCheckedChange = { open -> viewModel.update { it.copy(hasEnd = !open) } }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("All-day event", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.allDay,
                    onCheckedChange = { checked -> viewModel.update { it.copy(allDay = checked) } }
                )
            }

            Text("Progress unit", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DisplayUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = state.displayUnit == unit,
                        onClick = {
                            viewModel.update { s ->
                                s.copy(
                                    displayUnit = unit,
                                    remainingUnits = s.remainingUnits.filter { it != unit }
                                )
                            }
                        },
                        label = { Text(unitLabel(unit)) }
                    )
                }
            }

            Text("Also show remaining", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Up to 2 extras (3 lines total) beside the title on the widget",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DisplayUnit.entries.forEach { unit ->
                    if (unit == state.displayUnit) return@forEach
                    FilterChip(
                        selected = unit in state.remainingUnits,
                        onClick = { viewModel.toggleRemainingUnit(unit) },
                        label = { Text(unitLabel(unit)) }
                    )
                }
            }

            if (state.displayUnit == DisplayUnit.WEEKS || DisplayUnit.WEEKS in state.remainingUnits) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Weeks with 1 decimal", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.weeksOneDecimal,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(weeksOneDecimal = checked) }
                        }
                    )
                }
            }

            Text("Progress style", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressStyle.entries.forEach { style ->
                    FilterChip(
                        selected = state.progressStyle == style,
                        onClick = { viewModel.update { it.copy(progressStyle = style) } },
                        label = {
                            Text(
                                when (style) {
                                    ProgressStyle.BAR -> "Bar"
                                    ProgressStyle.DOTS -> "Dots"
                                    ProgressStyle.CIRCLE -> "Circle"
                                    ProgressStyle.SEGMENTS -> "Segments"
                                }
                            )
                        }
                    )
                }
            }

            if (state.displayUnit == DisplayUnit.WEEKS && state.progressStyle == ProgressStyle.DOTS) {
                Text("Current week", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeekDotMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.weekDotMode == mode,
                            onClick = { viewModel.update { it.copy(weekDotMode = mode) } },
                            label = {
                                Text(
                                    when (mode) {
                                        WeekDotMode.GREY_UNTIL_DONE -> "Grey until done"
                                        WeekDotMode.PARTIAL -> "Partial fill"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Text("Theme", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeStyle.entries.forEach { theme ->
                    FilterChip(
                        selected = state.theme == theme,
                        onClick = { viewModel.update { it.copy(theme = theme) } },
                        label = {
                            Text(
                                when (theme) {
                                    ThemeStyle.SWISS -> "Swiss"
                                    ThemeStyle.MINIMAL -> "Minimal"
                                    ThemeStyle.AQUA -> "Aqua"
                                    ThemeStyle.GRID -> "Grid"
                                    ThemeStyle.RETRO -> "Retro OS"
                                }
                            )
                        }
                    )
                }
            }

            Text("Accent", style = MaterialTheme.typography.titleLarge)
            ColorRow(accentChoices, state.accentColor) { color ->
                viewModel.update { it.copy(accentColor = color) }
            }

            Text("Background", style = MaterialTheme.typography.titleLarge)
            ColorRow(backgroundChoices, state.backgroundColor) { color ->
                val text = if (isLight(color)) 0xFF111111 else 0xFFFFFFFF
                viewModel.update { it.copy(backgroundColor = color, textColor = text) }
            }

            Text("Icon", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconChoices.forEach { key ->
                    FilterChip(
                        selected = state.iconKey == key,
                        onClick = { viewModel.update { it.copy(iconKey = key) } },
                        label = { Text(key.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show icon", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.showIcon,
                    onCheckedChange = { checked -> viewModel.update { it.copy(showIcon = checked) } }
                )
            }

            if (state.progressStyle != ProgressStyle.DOTS) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (state.progressStyle) {
                            ProgressStyle.CIRCLE -> "Rounded stroke"
                            else -> "Rounded progress bar"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.barRounded,
                        onCheckedChange = { checked -> viewModel.update { it.copy(barRounded = checked) } }
                    )
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { s -> s.copy(notes = it) } },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = onClick) {
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ColorRow(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { color ->
            val selectedBorder = if (selected == color) MaterialTheme.colorScheme.primary else Color.Transparent
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(2.dp, selectedBorder, CircleShape)
                    .clickable { onSelect(color) }
            )
        }
    }
}

private fun unitLabel(unit: DisplayUnit): String = when (unit) {
    DisplayUnit.DAYS -> "Days"
    DisplayUnit.WEEKS -> "Weeks"
    DisplayUnit.HOURS -> "Hours"
    DisplayUnit.DAYS_HOURS -> "Days + hours"
    DisplayUnit.PERCENT -> "Percent"
}

private fun isLight(color: Long): Boolean {
    val c = Color(color)
    val luminance = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return luminance > 0.65f
}

private fun pickDateTime(
    context: android.content.Context,
    initial: Long,
    allDay: Boolean,
    onPicked: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)
            if (allDay) {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                onPicked(cal.timeInMillis)
            } else {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            }
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
