package com.vacart.model

data class TrainRunningStatus(
    val trainNumber: String,
    val trainName: String,
    val currentStatus: String,
    val lastUpdatedOn: String,
    val startDate: String,
    val stops: List<StationStop>,
    // Enhanced fields from NTES JSON API
    val sourceStation: String = "",        // Origin station code
    val sourceStationName: String = "",    // Origin station full name
    val destStation: String = "",          // Destination station code
    val destStationName: String = "",      // Destination full name
    val totalDistance: String = "",        // Total route distance in km
    val currentDelayMins: Int? = null,     // Current overall delay in minutes
    val trainType: String = "",            // e.g. "Rajdhani", "Mail", "Express"
    val classes: String = "",             // e.g. "1A,2A,3A,SL"
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
    val haltMinutes: Int? = null,          // Scheduled halt duration in minutes
    val dayCount: Int = 0,                 // Day offset from journey start (0=day1, 1=day2...)
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
