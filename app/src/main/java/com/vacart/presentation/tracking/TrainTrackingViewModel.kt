package com.vacart.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vacart.model.TrainInfo
import com.vacart.repository.Result
import com.vacart.repository.TrainSearchManager
import com.vacart.repository.TrainTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainTrackingViewModel @Inject constructor(
    private val repository: TrainTrackingRepository,
    private val trainSearchManager: TrainSearchManager
) : ViewModel() {

    private val _state = MutableStateFlow(TrainTrackingState())
    val state: StateFlow<TrainTrackingState> = _state.asStateFlow()

    private var allTrains: List<TrainInfo> = emptyList()

    init {
        loadTrainList()
    }

    private fun loadTrainList() {
        viewModelScope.launch(Dispatchers.IO) {
            allTrains = trainSearchManager.getTrainList()
        }
    }

    fun onEvent(event: TrainTrackingEvent) {
        when (event) {
            is TrainTrackingEvent.UpdateTrainNo -> {
                updateTrainInput(event.trainNo)
            }
            is TrainTrackingEvent.SelectTrainInfo -> {
                selectTrainInfo(event.trainInfo)
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

    private fun updateTrainInput(text: String) {
        val extractedNumber = if (text.contains(" - ")) text.split(" - ")[0].trim() else text.trim()
        _state.value = _state.value.copy(
            selectedTrain = text,
            trainNoInput = extractedNumber,
            errorMessage = null
        )
        filterTrains(text)
    }

    private fun selectTrainInfo(trainInfo: TrainInfo) {
        _state.value = _state.value.copy(
            selectedTrain = trainInfo.rawDisplay,
            trainNoInput = trainInfo.trainNumber,
            filteredTrains = emptyList(),
            errorMessage = null
        )
    }

    private fun filterTrains(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val suggestions = trainSearchManager.searchTrains(allTrains, query)
            _state.value = _state.value.copy(filteredTrains = suggestions)
        }
    }

    private fun fetchStatus() {
        val trainNo = if (_state.value.trainNoInput.contains(" - ")) {
            _state.value.trainNoInput.split(" - ")[0].trim()
        } else {
            _state.value.trainNoInput.trim()
        }
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
        val trainNo = if (_state.value.trainNoInput.contains(" - ")) {
            _state.value.trainNoInput.split(" - ")[0].trim()
        } else {
            _state.value.trainNoInput.trim()
        }
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
