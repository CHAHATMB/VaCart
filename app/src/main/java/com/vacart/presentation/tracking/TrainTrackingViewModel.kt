package com.vacart.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vacart.model.TrainInfo
import com.vacart.model.TrainInstance
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
            is TrainTrackingEvent.FetchInstances -> fetchInstances()
            is TrainTrackingEvent.SelectInstance -> fetchStatusForInstance(event.instance)
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

    /**
     * Step 1: User taps "Find Instances" → call GetTrainInstance API
     * and transition to INSTANCE_LIST step on success.
     */
    private fun fetchInstances() {
        val trainNo = resolveTrainNo()
        if (trainNo.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Please enter a train number.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.fetchTrainInstances(trainNo)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "No active instances found for train $trainNo."
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            trainInstances = result.data,
                            step = TrackingStep.INSTANCE_LIST
                        )
                    }
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

    /**
     * Step 2: User selects an instance from the list → call ShowFullRunJson
     * and transition to RESULT step on success.
     */
    private fun fetchStatusForInstance(instance: TrainInstance) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.fetchTrainRunningStatus(instance.trainNo, instance.startDate)) {
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
        val status = _state.value.runningStatus ?: return
        val trainNo = status.trainNumber ?: resolveTrainNo()
        val date    = status.startDate   ?: return
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

    private fun resolveTrainNo(): String {
        val raw = _state.value.trainNoInput
        return if (raw.contains(" - ")) raw.split(" - ")[0].trim() else raw.trim()
    }
}
