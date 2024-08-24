package com.example.vacart.model

data class ChartStatusResponseDtoX(
    val chartOneFlag: Int,
    val chartTwoFlag: Int,
    val messageIndex: Int,
    val messageType: String,
    val remoteStationCode: String,
    val trainStartDate: String
)