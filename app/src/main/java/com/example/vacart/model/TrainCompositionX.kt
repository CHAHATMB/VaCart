package com.example.vacart.model

data class TrainCompositionX(
    val avlRemoteForBooking: Any,
    val cdd: List<CddX>,
    val chartOneDate: String,
    val chartStatusResponseDto: ChartStatusResponseDtoX,
    val chartTwoDate: Any,
    val destinationStation: Any,
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