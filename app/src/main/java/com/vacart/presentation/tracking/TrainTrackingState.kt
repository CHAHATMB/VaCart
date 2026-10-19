package com.vacart.presentation.tracking

import com.vacart.model.TrainInfo
import com.vacart.model.TrainInstance
import com.vacart.model.TrainRunningStatus

data class TrainTrackingState(
    val trainNoInput: String = "",
    val selectedTrain: String = "",
    val filteredTrains: List<TrainInfo> = emptyList(),
    val trainInstances: List<TrainInstance> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val runningStatus: TrainRunningStatus? = null,
    val errorMessage: String? = null,
    val step: TrackingStep = TrackingStep.INPUT
)

enum class TrackingStep {
    INPUT,
    INSTANCE_LIST,
    RESULT
}
