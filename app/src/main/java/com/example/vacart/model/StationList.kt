package com.example.vacart.model

data class StationList(
    val duration: String?,
    val stationFrom: String?,
    val stationList: List<Station>?,
    val stationTo: String?,
    val timeStamp: String?,
    val trainName: String?,
    val trainNumber: String?,
    val trainOwner: String?,
    val trainRunsOnFri: String?,
    val trainRunsOnMon: String?,
    val trainRunsOnSat: String?,
    val trainRunsOnSun: String?,
    val trainRunsOnThu: String?,
    val trainRunsOnTue: String?,
    val trainRunsOnWed: String?
)