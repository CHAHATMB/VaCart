package com.vacart.model

/**
 * Represents a single active running instance of a train returned by the
 * `GetTrainInstance` NTES API endpoint.
 *
 * @param startDate       Journey start date in dd-MMM-yyyy format (e.g. "19-Oct-2026")
 * @param trainPosition   Human-readable position description from NTES
 * @param trainStatus     0 = Yet to start, 1 = In transit, 2 = Completed
 * @param trainNo         Train number
 * @param trainName       English train name
 * @param srcCode         Source station code
 * @param srcName         Source station name
 * @param dstnCode        Destination station code
 * @param dstnName        Destination station name
 * @param excpMsg         Exception / alert message (may be blank)
 */
data class TrainInstance(
    val startDate: String,
    val trainPosition: String,
    val trainStatus: Int,
    val trainNo: String,
    val trainName: String,
    val srcCode: String,
    val srcName: String,
    val dstnCode: String,
    val dstnName: String,
    val excpMsg: String = ""
)
