package com.vacart.presentation.home

import com.vacart.model.CoachComposition
import com.vacart.model.StationList
import com.vacart.model.TrainComposition
import com.vacart.model.TrainInfo
import com.vacart.model.VacantBerth
import com.vacart.model.Vbd

data class HomeState(
    var filteredTrains: List<TrainInfo> = emptyList(),

    var showStation: Boolean = false,
    var selectedTrain: String = "",
    var trainNumber: String = "23",
    var stationList: StationList? = null,
    var stationOptions: List<String> = emptyList(),
    var journeyDate: String = "",
    var boardingStation: String = "",
    var trainComposition: TrainComposition? = null,
    var coachComposition: CoachComposition? = null,
    var selectedDateString: String = "",
    var isLoading: Boolean = true,
    var showError: Boolean = false,
    var errorMessage: String? = null,
    var searchQuery: String = "",
    var vacantBerth: VacantBerth? = null,
    var vacantBerthList: List<Vbd> = emptyList(),
    var selectedClassCode: String = "",
    var isAscending: Boolean = true,
    var lastSortedColumn: String = "",
    var selectedCoach: String = ""
){
    constructor(trainComposition: TrainComposition) : this(emptyList()) {
        this.trainComposition = trainComposition
    }
}