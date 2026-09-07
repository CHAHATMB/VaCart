package com.vacart.presentation.home

import com.vacart.model.TrainInfo

sealed class HomeEvent {
    data class trainFilter(var selectText: String) : HomeEvent()
    data class selectTrain(var selectText: String) : HomeEvent()
    data class selectTrainInfo(val trainInfo: TrainInfo) : HomeEvent()
    data class getTrainComposition(var selectText: String) : HomeEvent()
    data class getClassDetail(var selectText: String) : HomeEvent()
    data class updateTrainNumber(var trainNumber: String) : HomeEvent()
    data class updateSelectDate(var dateString: String) : HomeEvent()
    data class sortByColumn(var column: String) : HomeEvent()
    data class updateSearchQuery(var searchQuery: String) : HomeEvent()
    data class selectClassCode(var classCode: String) : HomeEvent()
    class getVacantBerth() : HomeEvent()
    data class selectCoach(var coach: String) : HomeEvent()
    class getCoachComposition() : HomeEvent()
    data class fetchStationList(var trainNumber: String = "") : HomeEvent()
}