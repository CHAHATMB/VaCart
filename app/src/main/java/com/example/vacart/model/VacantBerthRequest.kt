package com.example.vacart.model

data class VacantBerthRequest(
    val boardingStation: String,
    val chartType: Int,
    val cls: String,
    val jDate: String,
    val remoteStation: String,
    val trainNo: String,
    val trainSourceStation: String
)