package com.vacart.presentation.tracking

import com.vacart.model.TrainInfo

sealed class TrainTrackingEvent {
    data class UpdateTrainNo(val trainNo: String) : TrainTrackingEvent()
    data class SelectTrainInfo(val trainInfo: TrainInfo) : TrainTrackingEvent()
    data class UpdateDate(val date: String) : TrainTrackingEvent()
    data class UpdateDateOption(val label: String, val formattedDate: String) : TrainTrackingEvent()
    object Search : TrainTrackingEvent()
    object Reset : TrainTrackingEvent()
    object Refresh : TrainTrackingEvent()
}
