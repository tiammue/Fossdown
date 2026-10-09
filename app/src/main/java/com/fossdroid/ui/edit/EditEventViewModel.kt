package com.fossdroid.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fossdroid.data.DisplayUnit
import com.fossdroid.data.EventMode
import com.fossdroid.data.EventRepository
import com.fossdroid.data.ProgressEvent
import com.fossdroid.data.ProgressStyle
import com.fossdroid.data.ThemeStyle
import com.fossdroid.data.WeekDotMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class EditEventUiState(
    val id: Long = 0,
    val title: String = "",
    val mode: EventMode = EventMode.COUNTDOWN,
    val startMillis: Long = System.currentTimeMillis(),
    val endMillis: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
    val hasEnd: Boolean = true,
    val allDay: Boolean = true,
    val theme: ThemeStyle = ThemeStyle.MINIMAL,
    val accentColor: Long = 0xFF7CFFB2,
    val backgroundColor: Long = 0xFF121212,
    val textColor: Long = 0xFFFFFFFF,
    val displayUnit: DisplayUnit = DisplayUnit.DAYS,
    val remainingUnits: List<DisplayUnit> = emptyList(),
    val weeksOneDecimal: Boolean = false,
    val showIcon: Boolean = true,
    val iconKey: String = "flag",
    val progressStyle: ProgressStyle = ProgressStyle.BAR,
    val weekDotMode: WeekDotMode = WeekDotMode.PARTIAL,
    val barRounded: Boolean = true,
    val notes: String = "",
    val loaded: Boolean = false
)

class EditEventViewModel(
    private val repository: EventRepository,
    private val eventId: Long
) : ViewModel() {
    private val _state = MutableStateFlow(EditEventUiState())
    val state: StateFlow<EditEventUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (eventId > 0) {
                repository.getById(eventId)?.let { event ->
                    _state.value = event.toUiState().copy(loaded = true)
                } ?: run { _state.update { it.copy(loaded = true) } }
            } else {
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    fun update(transform: (EditEventUiState) -> EditEventUiState) {
        _state.update(transform)
    }

    fun toggleRemainingUnit(unit: DisplayUnit) {
        _state.update { state ->
            if (unit == state.displayUnit) return@update state
            val next = state.remainingUnits.toMutableList()
            if (unit in next) {
                next.remove(unit)
            } else {
                // Title row can show progress unit + 2 extras = 3 lines total.
                if (next.size >= 2) return@update state
                next.add(unit)
            }
            state.copy(remainingUnits = next)
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            val event = ProgressEvent(
                id = if (eventId > 0) eventId else 0,
                title = s.title.ifBlank { "Untitled" },
                mode = s.mode,
                startEpochMillis = s.startMillis,
                endEpochMillis = when {
                    s.mode == EventMode.COUNT_UP && !s.hasEnd -> null
                    else -> s.endMillis
                },
                allDay = s.allDay,
                theme = s.theme,
                accentColor = s.accentColor,
                backgroundColor = s.backgroundColor,
                textColor = s.textColor,
                displayUnit = s.displayUnit,
                remainingUnits = s.remainingUnits.filter { it != s.displayUnit }.take(2),
                weeksOneDecimal = s.weeksOneDecimal,
                showIcon = s.showIcon,
                iconKey = s.iconKey,
                progressStyle = s.progressStyle,
                weekDotMode = s.weekDotMode,
                barRounded = s.barRounded,
                notes = s.notes
            )
            repository.save(event)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (eventId <= 0) {
            onDeleted()
            return
        }
        viewModelScope.launch {
            repository.getById(eventId)?.let { repository.delete(it) }
            onDeleted()
        }
    }

    companion object {
        fun factory(repository: EventRepository, eventId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditEventViewModel(repository, eventId) as T
                }
            }
    }
}

private fun ProgressEvent.toUiState() = EditEventUiState(
    id = id,
    title = title,
    mode = mode,
    startMillis = startEpochMillis,
    endMillis = endEpochMillis ?: (startEpochMillis + TimeUnit.DAYS.toMillis(30)),
    hasEnd = endEpochMillis != null,
    allDay = allDay,
    theme = theme,
    accentColor = accentColor,
    backgroundColor = backgroundColor,
    textColor = textColor,
    displayUnit = displayUnit,
    remainingUnits = remainingUnits.filter { it != displayUnit },
    weeksOneDecimal = weeksOneDecimal,
    showIcon = showIcon,
    iconKey = iconKey,
    progressStyle = progressStyle,
    weekDotMode = weekDotMode,
    barRounded = barRounded,
    notes = notes
)
