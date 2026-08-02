package com.vacart.model

data class TrainComposition(
    val avlRemoteForBooking: String? = null,
    val cdd: List<Cdd>? = null,
    val chartOneDate: String? = null,
    val chartStatusResponseDto: ChartStatusResponseDto? = null,
    val chartTwoDate: String? = null,
    val destinationStation: String? = null,
    val error: Any? = null,
    val errorMessage: String? = null,
    val from: String? = null,
    val nextRemote: String? = null,
    val remote: String? = null,
    val remoteLocationChartDate: String? = null,
    val to: String? = null,
    val trainName: String? = null,
    val trainNo: String? = null,
    val trainStartDate: String? = null
) {
    constructor(cdd: List<Cdd>): this(
        avlRemoteForBooking = "",
        cdd = cdd,
        chartOneDate = "",
        chartStatusResponseDto = ChartStatusResponseDto(1, 1, 1, "", "", ""),
        chartTwoDate = "",
        destinationStation = "",
        error = null,
        errorMessage = null,
        from = "",
        nextRemote = "",
        remote = "",
        remoteLocationChartDate = "",
        to = "",
        trainName = "",
        trainNo = "",
        trainStartDate = ""
    )
}