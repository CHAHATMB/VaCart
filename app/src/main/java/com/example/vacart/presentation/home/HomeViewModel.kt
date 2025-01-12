package com.example.vacart.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vacart.model.CoachCompositionRequest
import com.example.vacart.model.TrainInfoRequest
import com.example.vacart.model.VacantBerthRequest
import com.example.vacart.repository.Result
import com.example.vacart.repository.TrainRepository
import com.example.vacart.util.trainList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val trainRepository: TrainRepository): ViewModel() {

    var _state = MutableStateFlow(HomeState())
    var state: StateFlow<HomeState> = _state.asStateFlow()

    fun onEvent(event: HomeEvent){
        when(event){
            is HomeEvent.trainFilter -> {
                filterTrains(event.selectText)
            }
            is HomeEvent.selectTrain ->{
                getStationList(event.selectText)
            }
            is HomeEvent.getTrainComposition -> {
                getTrainComposition()
            }
            is HomeEvent.updateTrainNumber -> {
                updateTrainNumber(event.trainNumber)
            }
            is HomeEvent.updateSelectDate -> {
                updateDateString(event.dateString)
            }
            is HomeEvent.sortByColumn -> {
                sortByColumn(event.column)
            }
            is HomeEvent.updateSearchQuery -> {
                updateSearchQuery(event.searchQuery)
            }
            is HomeEvent.selectClassCode -> {
                selectClassCode(event.classCode)
            }
            is HomeEvent.getVacantBerth -> {
                getVacantBerth()
            }
            is HomeEvent.selectCoach -> {
                selectCoach(event.coach)
            }
            is HomeEvent.getCoachComposition -> {
                getCoachComposition()
            }
            else -> {}
        }
    }

    private fun filterTrains(selectedText: String) {
        if(selectedText.length >2 ) {
            viewModelScope.launch {
            _state.value.filteredTrains = trainList.filter { it.contains(selectedText, ignoreCase = true) }
                println("event change happens - ${_state.value.filteredTrains}")
            }
        }
    }

    private fun getStationList(selectedText: String){
        _state.value.trainNumber = selectedText.substring(0,5)
        println("Trian number ${_state.value.trainNumber}")
    }

    private fun getTrainComposition(){
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            when (val apiResult = trainRepository.getStationList(_state.value.trainNumber)) {
                is Result.Success -> {
                    _state.value.stationList = apiResult.data
                }
                is Result.Error -> {
                    TODO()
                }
                else -> {
                    println("Do nothing!")
                }
            }
            println("value in station list ${_state.value.stationList}")
            _state.value.boardingStation = _state.value.stationList?.stationList?.get(0)?.stationCode.toString();
            println("Boarding station ${_state.value.boardingStation}")
            val trainInfoRequest = TrainInfoRequest(_state.value.boardingStation,_state.value.journeyDate,_state.value.trainNumber)
            when (val apiResult = trainRepository.getTrainComposition(trainInfoRequest)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(trainComposition = apiResult.data, isLoading = false)
                }
                is Result.Error -> {
                    TODO()
                }
                else -> {
                    println("Do nothing!")
                }
            }



            println("Train composition - ${_state.value.trainComposition}")
        }
    }
    private fun updateTrainNumber(trainNumber: String){
        _state.value = _state.value.copy(trainNumber = trainNumber)
        println("updating train number - ${_state.value.trainNumber}")
    }
    private fun updateDateString(date: String){
        _state.value = _state.value.copy(selectedDateString = date)
    }
    private fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    private fun sortByColumn(column: String) {
        val currentOrder = if (_state.value.lastSortedColumn == column) _state.value.isAscending else true
        val sortedList = when (column) {
            "fromStation" -> if (currentOrder) _state.value.vacantBerth!!.vbd.sortedBy { it.from }else _state.value.vacantBerth!!.vbd.sortedByDescending{ it.from }
            "toStation" -> if (currentOrder) _state.value.vacantBerth!!.vbd.sortedBy { it.to }else _state.value.vacantBerth!!.vbd.sortedByDescending{ it.to }
            "coach" -> if (currentOrder) _state.value.vacantBerth!!.vbd.sortedBy { it.coachName }else _state.value.vacantBerth!!.vbd.sortedByDescending{ it.coachName }
            else -> _state.value.vacantBerth!!.vbd
        }

        _state.value = _state.value.copy(
            vacantBerthList = sortedList,
            isAscending = !currentOrder,
            lastSortedColumn = column
        )
    }


    private fun selectClassCode(classCode: String) {
        _state.value = _state.value.copy(selectedClassCode = classCode)
    }

    private fun getVacantBerth(){
        viewModelScope.launch {
            var vacantBerthRequest = VacantBerthRequest(
                boardingStation = _state.value.boardingStation,
                chartType = 1, cls = _state.value.selectedClassCode,
                jDate = _state.value.journeyDate,
                remoteStation = _state.value.boardingStation,
                trainNo = _state.value.trainNumber,
                trainSourceStation = _state.value.boardingStation
            )
            _state.value = state.value.copy(isLoading = true)
            when (val apiResult = trainRepository.getVacantBerth(vacantBerthRequest)) {
                is Result.Success -> {
                    _state.value = state.value.copy(vacantBerth = apiResult.data, isLoading = false)
                }

                is Result.Error -> {
                    TODO()
                }

                else -> {
                    println("Do nothing!")
                }
            }
            println("vacant birth list ${_state.value.vacantBerth}")
           _state.value = state.value.copy(vacantBerthList = _state.value.vacantBerth!!.vbd)
        }
    }

    private fun selectCoach(coach: String) {
        _state.value = _state.value.copy(selectedCoach = coach)
    }

    private fun getCoachComposition(){
        viewModelScope.launch {
            var coachCompositionRequest = CoachCompositionRequest(
                boardingStation = _state.value.boardingStation,
                cls = _state.value.selectedClassCode,
                jDate = _state.value.journeyDate,
                remoteStation = _state.value.boardingStation,
                trainNo = _state.value.trainNumber,
                coach = _state.value.selectedCoach,
                trainSourceStation = _state.value.boardingStation
            )
            _state.value = state.value.copy(isLoading = true)
            when (val apiResult = trainRepository.getCoachComposition(coachCompositionRequest)) {
                is Result.Success -> {
                    _state.value = state.value.copy(coachComposition = apiResult.data, isLoading = false)
                }

                is Result.Error -> {
                    TODO()
                }

                else -> {
                    println("Do nothing!")
                }
            }
        }
    }

    public fun getStationName(stationCode: String): String {
        return state.value.stationList?.stationList?.filter { station ->
            station.stationCode == stationCode
        }?.get(0)?.stationName ?: stationCode
    }
}