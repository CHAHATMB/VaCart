package com.vacart.presentation.tracking

import com.vacart.model.TrainInfo
import com.vacart.model.TrainRunningStatus
import com.vacart.util.getFormattedDateForNtes

data class TrainTrackingState(
    val trainNoInput: String = "",
    val selectedTrain: String = "",
    val filteredTrains: List<TrainInfo> = emptyList(),
    val dateLabel: String = "Today",
    val dateInput: String = getFormattedDateForNtes(0),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val runningStatus: TrainRunningStatus? = null,
    val errorMessage: String? = null,
    val step: TrackingStep = TrackingStep.INPUT
)

enum class TrackingStep {
    INPUT,
    RESULT
}
