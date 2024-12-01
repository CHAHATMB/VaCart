package com.example.vacart.model

data class CoachCompositionRequest(
    val boardingStation: String,
    val cls: String,
    val coach: String,
    val jDate: String,
    val remoteStation: String,
    val trainNo: String,
    val trainSourceStation: String
)