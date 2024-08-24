package com.example.vacart.presentation.home

import com.example.vacart.model.CoachComposition
import com.example.vacart.model.Station
import com.example.vacart.model.StationList
import com.example.vacart.model.TrainComposition
import com.example.vacart.model.VacantBerth
import com.example.vacart.model.Vbd

data class HomeState(
    var filteredTrains: List<String> = emptyList(),
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
    var searchQuery: String = "",
    var vacantBerth: VacantBerth? = null,
    var vacantBerthList: List<Vbd> = emptyList(),
    var selectedClassCode: String = "",
    var isAscending: Boolean = true,
    var lastSortedColumn: String = "",
    var selectedCoach: String = ""
)