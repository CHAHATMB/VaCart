package com.vacart.model

data class StationList(
    val duration: String? = null,
    val errorMessage: String? = null,
    val serverId: String? = null,
    val stationFrom: String? = null,
    val stationList: List<Station>? = null,
    val stationTo: String? = null,
    val timeStamp: String? = null,
    val trainName: String? = null,
    val trainNumber: String? = null,
    val trainOwner: String? = null,
    val trainRunsOnFri: String? = null,
    val trainRunsOnMon: String? = null,
    val trainRunsOnSat: String? = null,
    val trainRunsOnSun: String? = null,
    val trainRunsOnThu: String? = null,
    val trainRunsOnTue: String? = null,
    val trainRunsOnWed: String? = null
)