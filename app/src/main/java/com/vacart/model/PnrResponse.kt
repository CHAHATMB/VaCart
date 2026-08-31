package com.vacart.model

data class PnrResponse(
    val pnrNumber: String = "",
    val dateOfJourney: String = "",
    val trainNumber: String = "",
    val trainName: String = "",
    val sourceStation: String = "",
    val destinationStation: String = "",
    val reservationUpto: String = "",
    val boardingPoint: String = "",
    val journeyClass: String = "",
    val numberOfpassenger: Int = 0,
    val chartStatus: String = "",
    val informationMessage: List<String> = emptyList(),
    val passengerList: List<PnrPassenger> = emptyList(),
    val timeStamp: String = "",
    val bookingFare: Int = 0,
    val ticketFare: Int = 0,
    val quota: String = "",
    val vikalpStatus: String = "",
    val bookingDate: String = "",
    val arrivalDate: String = "",
    val distance: Int = 0,
    val isWL: String = "N"
)

data class PnrPassenger(
    val passengerSerialNumber: Int = 0,
    val passengerQuota: String = "",
    val bookingStatus: String = "",
    val bookingCoachId: String = "",
    val bookingBerthNo: Int = 0,
    val bookingBerthCode: String = "",
    val bookingStatusDetails: String = "",
    val currentStatus: String = "",
    val currentCoachId: String = "",
    val currentBerthNo: Int = 0,
    val currentBerthCode: String = "",
    val currentStatusDetails: String = ""
)
