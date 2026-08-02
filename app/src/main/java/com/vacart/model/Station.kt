package com.vacart.model

data class Station(
    val arrivalTime: String? = "--",
    val boardingDisabled: String? = "false",
    val dayCount: String? = "1",
    val departureTime: String? = "--",
    val distance: String? = "0",
    val haltTime: String? = "--",
    val routeNumber: String? = "1",
    val stationCode: String? = "",
    val stationName: String? = "",
    val stnSerialNumber: String? = "1",
    val status: String? = null
)