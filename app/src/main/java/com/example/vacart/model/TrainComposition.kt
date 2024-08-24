package com.example.vacart.model

data class TrainComposition(
    val avlRemoteForBooking: String,
    val cdd: List<Cdd>,
    val chartOneDate: String,
    val chartStatusResponseDto: ChartStatusResponseDto,
    val chartTwoDate: String,
    val destinationStation: String,
    val error: Any,
    val from: String,
    val nextRemote: String,
    val remote: String,
    val remoteLocationChartDate: String,
    val to: String,
    val trainName: String,
    val trainNo: String,
    val trainStartDate: String
)