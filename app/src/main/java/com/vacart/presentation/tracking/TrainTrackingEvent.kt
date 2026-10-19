package com.vacart.presentation.tracking

import com.vacart.model.TrainInfo
import com.vacart.model.TrainInstance

sealed class TrainTrackingEvent {
    data class UpdateTrainNo(val trainNo: String) : TrainTrackingEvent()
    data class SelectTrainInfo(val trainInfo: TrainInfo) : TrainTrackingEvent()
    /** Triggered when user taps "Find Instances" — calls GetTrainInstance API */
    object FetchInstances : TrainTrackingEvent()
    /** Triggered when user picks an instance from the list */
    data class SelectInstance(val instance: TrainInstance) : TrainTrackingEvent()
    object Refresh : TrainTrackingEvent()
    object Reset : TrainTrackingEvent()
}
