package com.vacart.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vacart.repository.Result
import com.vacart.repository.TrainTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainTrackingViewModel @Inject constructor(
    private val repository: TrainTrackingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrainTrackingState())
    val state: StateFlow<TrainTrackingState> = _state.asStateFlow()

    fun onEvent(event: TrainTrackingEvent) {
        when (event) {
            is TrainTrackingEvent.UpdateTrainNo -> {
                _state.value = _state.value.copy(trainNoInput = event.trainNo, errorMessage = null)
            }
            is TrainTrackingEvent.UpdateDate -> {
                _state.value = _state.value.copy(dateInput = event.date, errorMessage = null)
            }
            is TrainTrackingEvent.UpdateDateOption -> {
                _state.value = _state.value.copy(
                    dateLabel = event.label,
                    dateInput = event.formattedDate,
                    errorMessage = null
                )
            }
            is TrainTrackingEvent.Search -> fetchStatus()
            is TrainTrackingEvent.Refresh -> refreshStatus()
            is TrainTrackingEvent.Reset -> {
                _state.value = TrainTrackingState()
            }
        }
    }

    private fun fetchStatus() {
        val trainNo = _state.value.trainNoInput.trim()
        val date = _state.value.dateInput.trim()
        if (trainNo.isBlank() || date.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Please enter both train number and date.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.fetchTrainRunningStatus(trainNo, date)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        runningStatus = result.data,
                        step = TrackingStep.RESULT
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Something went wrong."
                    )
                }
                else -> _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun refreshStatus() {
        val trainNo = _state.value.trainNoInput.trim()
        val date = _state.value.dateInput.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            when (val result = repository.fetchTrainRunningStatus(trainNo, date)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        runningStatus = result.data
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        errorMessage = result.exception.message ?: "Refresh failed."
                    )
                }
                else -> _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }
}
