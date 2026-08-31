package com.vacart.model

data class TrainRunningStatus(
    val trainNumber: String,
    val trainName: String,
    val currentStatus: String,
    val lastUpdatedOn: String,
    val startDate: String,
    val stops: List<StationStop>
)

data class CoachPositionInfo(
    val coachType: String,
    val coachName: String,
    val positionIndex: String
)

data class StationStop(
    val stationCode: String,
    val stationName: String,
    val scheduledArrival: String = "",
    val actualArrival: String = "",
    val arrivalDelay: String = "",
    val scheduledDeparture: String = "",
    val actualDeparture: String = "",
    val departureDelay: String = "",
    val platform: String = "",
    val distance: String = "",
    val isStop: Boolean = true,
    val delayMinutes: Int? = null,
    val status: StopStatus = StopStatus.UPCOMING,
    val isLiveLocation: Boolean = false,
    val updatedOn: String = "",
    val liveStatusText: String = "",
    val coachPositions: List<CoachPositionInfo> = emptyList(),
    val divyangjanInfo: String = ""
)

enum class StopStatus {
    DEPARTED,
    AT_STATION,
    UPCOMING,
    SKIPPED
}

