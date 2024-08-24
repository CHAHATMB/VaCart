package com.example.vacart.presentation.home

sealed class HomeEvent {
    data class trainFilter(var selectText: String): HomeEvent()
    data class selectTrain(var selectText: String): HomeEvent() // rename it to get stations
    data class getTrainComposition(var selectText: String): HomeEvent()
    data class getClassDetail(var selectText: String): HomeEvent()  //vacant birthAPI
    data class updateTrainNumber(var trainNumber: String): HomeEvent()
    data class updateSelectDate(var dateString: String): HomeEvent()
    data class sortByColumn(var column: String): HomeEvent()
    data class updateSearchQuery(var searchQuery: String): HomeEvent()
    data class selectClassCode(var classCode: String): HomeEvent()
    class getVacantBerth(): HomeEvent()
    data class selectCoach(var coach: String): HomeEvent()
    class getCoachComposition(): HomeEvent()
}