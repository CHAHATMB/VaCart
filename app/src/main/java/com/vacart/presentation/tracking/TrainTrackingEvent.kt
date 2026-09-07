package com.vacart.presentation.tracking

sealed class TrainTrackingEvent {
    data class UpdateTrainNo(val trainNo: String) : TrainTrackingEvent()
    data class UpdateDate(val date: String) : TrainTrackingEvent()
    data class UpdateDateOption(val label: String, val formattedDate: String) : TrainTrackingEvent()
    object Search : TrainTrackingEvent()
    object Reset : TrainTrackingEvent()
    object Refresh : TrainTrackingEvent()
}
