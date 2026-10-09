package com.fossdown.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fossdown.BuildConfig
import com.fossdown.data.EventRepository
import com.fossdown.data.ProgressEvent
import com.fossdown.update.AppUpdate
import com.fossdown.update.UpdateCheckResult
import com.fossdown.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class UpToDate(val version: String) : UpdateUiState()
    data class Available(val update: AppUpdate, val showDialog: Boolean) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

class HomeViewModel(
    private val repository: EventRepository
) : ViewModel() {
    val events: StateFlow<List<ProgressEvent>> = repository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME
    val releasesUrl: String = BuildConfig.GITHUB_RELEASES_URL

    init {
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            if (manual) {
                _updateState.value = UpdateUiState.Checking
            }
            when (val result = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateState.value = UpdateUiState.Available(
                        update = result.update,
                        showDialog = manual
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateState.value = if (manual) {
                        UpdateUiState.UpToDate(result.currentVersion)
                    } else {
                        UpdateUiState.Idle
                    }
                }
                is UpdateCheckResult.Failed -> {
                    _updateState.value = if (manual) {
                        UpdateUiState.Error(result.message)
                    } else {
                        UpdateUiState.Idle
                    }
                }
            }
        }
    }

    fun dismissUpdateMessage() {
        when (val current = _updateState.value) {
            is UpdateUiState.UpToDate, is UpdateUiState.Error -> {
                _updateState.value = UpdateUiState.Idle
            }
            is UpdateUiState.Available -> {
                _updateState.value = current.copy(showDialog = false)
            }
            else -> Unit
        }
    }

    fun delete(event: ProgressEvent) {
        viewModelScope.launch { repository.delete(event) }
    }

    companion object {
        fun factory(repository: EventRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
