package com.vacart.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vacart.model.CoachComposition
import com.vacart.model.CoachCompositionRequest
import com.vacart.model.TrainInfo
import com.vacart.model.TrainInfoRequest
import com.vacart.model.VacantBerth
import com.vacart.model.VacantBerthRequest
import com.vacart.repository.Result
import com.vacart.repository.TrainRepository
import com.vacart.repository.TrainSearchManager
import com.vacart.roomdatabase.SearchDao
import com.vacart.roomdatabase.SearchEntity
import com.vacart.roomdatabase.SearchEntityKey
import com.vacart.roomdatabase.VacartCacheEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainRepository: TrainRepository,
    private val trainSearchManager: TrainSearchManager,
    private val searchDao: SearchDao
) : ViewModel() {

    var _state = MutableStateFlow(HomeState())
    var state: StateFlow<HomeState> = _state.asStateFlow()

    private var allTrains: List<TrainInfo> = emptyList()
    private val gson = Gson()

    init {
        loadTrainList()
    }

    private fun loadTrainList() {
        viewModelScope.launch(Dispatchers.IO) {
            allTrains = trainSearchManager.getTrainList()
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.trainFilter -> {
                filterTrains(event.selectText)
            }
            is HomeEvent.selectTrainInfo -> {
                selectTrainInfo(event.trainInfo)
            }
            is HomeEvent.selectTrain -> {
                getStationList(event.selectText)
            }
            is HomeEvent.getTrainComposition -> {
                getTrainComposition()
            }
            is HomeEvent.updateTrainNumber -> {
                updateTrainInput(event.trainNumber)
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
            is HomeEvent.fetchStationList -> {
                fetchStationList(event.trainNumber)
            }
            else -> {}
        }
    }

    // Recent searches from the database
    val recentSearches = searchDao.getRecentSearches()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun saveSearch(trainNumber: String, journeyDate: String) {
        viewModelScope.launch {
            searchDao.insertSearch(
                SearchEntity(
                    trainNumber = trainNumber,
                    journeyDate = journeyDate,
                    searchEntityKey = SearchEntityKey(trainNumber = trainNumber, journeyDate = journeyDate)
                )
            )
        }
    }

    private fun filterTrains(selectedText: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val suggestions = trainSearchManager.searchTrains(allTrains, selectedText)
            _state.value = _state.value.copy(filteredTrains = suggestions)
        }
    }

    private fun selectTrainInfo(trainInfo: TrainInfo) {
        _state.value = _state.value.copy(
            selectedTrain = trainInfo.rawDisplay,
            trainNumber = trainInfo.trainNumber,
            filteredTrains = emptyList()
        )
    }

    private fun updateTrainInput(text: String) {
        val extractedNumber = if (text.contains(" - ")) text.split(" - ")[0].trim() else text.trim()
        _state.value = _state.value.copy(
            selectedTrain = text,
            trainNumber = extractedNumber
        )
        filterTrains(text)
    }

    private fun getStationList(selectedText: String) {
        val extractedNumber = if (selectedText.contains(" - ")) selectedText.split(" - ")[0].trim() else selectedText.take(5)
        _state.value = _state.value.copy(trainNumber = extractedNumber)
    }

    fun fetchStationList(trainNum: String = "") {
        val targetTrain = trainNum.ifEmpty { _state.value.trainNumber }.trim()
        if (targetTrain.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (_state.value.stationList?.trainNumber != targetTrain || _state.value.stationList?.stationList.isNullOrEmpty()) {
                _state.value = _state.value.copy(isLoading = true)
                when (val apiResult = trainRepository.getStationList(targetTrain)) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            stationList = apiResult.data,
                            trainNumber = targetTrain,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(showError = true, isLoading = false)
                    }
                    else -> {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun getTrainComposition() {
        viewModelScope.launch {
            val trainNumber = _state.value.trainNumber
            val journeyDate = _state.value.journeyDate
            val cacheKey = "${trainNumber}_${journeyDate}"

            _state.value = state.value.copy(
                isLoading = true,
                showError = false,
                errorMessage = null,
                isOfflineData = false,
                isStaleData = false,
                offlineCachedAt = null
            )

            // Step 1: Get station list (needed for boardingStation)
            when (val apiResult = trainRepository.getStationList(trainNumber)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(stationList = apiResult.data)
                }
                is Result.Error -> {
                    // Station-list API failed (likely offline) — try full cache entry
                    val cached = trainRepository.getCachedVacartEntry(cacheKey)
                    if (cached != null) {
                        _state.value = _state.value.copy(
                            trainComposition = cached.trainComposition,
                            boardingStation = cached.boardingStation,
                            isLoading = false,
                            isOfflineData = true,
                            isStaleData = cached.isStale,
                            offlineCachedAt = cached.cachedAt
                        )
                    } else {
                        _state.value = _state.value.copy(
                            showError = true,
                            errorMessage = apiResult.exception.message,
                            isLoading = false
                        )
                    }
                    return@launch
                }
                else -> {}
            }

            // Step 2: Derive boarding station, fetch train composition
            _state.value.boardingStation = _state.value.stationList?.stationList?.getOrNull(0)?.stationCode.toString()
            val trainInfoRequest = TrainInfoRequest(
                _state.value.boardingStation,
                journeyDate,
                trainNumber
            )
            when (val apiResult = trainRepository.getTrainComposition(trainInfoRequest)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(trainComposition = apiResult.data, isLoading = false)
                    // Background prefetch: fetch all class VacantBerths + all coach CoachCompositions
                    prefetchVacartData(
                        trainNumber = trainNumber,
                        journeyDate = journeyDate,
                        boardingStation = _state.value.boardingStation
                    )
                }
                is Result.Error -> {
                    // Train-composition API failed — try cache before showing error
                    val cached = trainRepository.getCachedVacartEntry(cacheKey)
                    if (cached != null) {
                        _state.value = _state.value.copy(
                            trainComposition = cached.trainComposition,
                            boardingStation = cached.boardingStation,
                            isLoading = false,
                            isOfflineData = true,
                            isStaleData = cached.isStale,
                            offlineCachedAt = cached.cachedAt
                        )
                    } else {
                        _state.value = _state.value.copy(
                            showError = true,
                            errorMessage = apiResult.exception.message,
                            isLoading = false
                        )
                    }
                }
                else -> {}
            }
        }
    }


    /**
     * Fire-and-forget background prefetch of all VacantBerth (per class) and
     * CoachComposition (per coach) responses. Bundles everything into one
     * [VacartCacheEntity] and upserts it so offline lookups find it immediately.
     */
    private fun prefetchVacartData(trainNumber: String, journeyDate: String, boardingStation: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val composition = _state.value.trainComposition ?: return@launch
            val classCodes = composition.cdd?.map { it.classCode } ?: return@launch

            // Fetch VacantBerth for every class code
            val allVacantBerths = mutableMapOf<String, VacantBerth>()
            classCodes.forEach { cls ->
                val result = trainRepository.getVacantBerth(
                    VacantBerthRequest(
                        boardingStation = boardingStation,
                        chartType = 1,
                        cls = cls,
                        jDate = journeyDate,
                        remoteStation = boardingStation,
                        trainNo = trainNumber,
                        trainSourceStation = boardingStation
                    )
                )
                if (result is Result.Success) allVacantBerths[cls] = result.data
            }

            // Fetch CoachComposition for every coach name
            val allCoaches = composition.cdd.map { it.coachName }
            val allCoachCompositions = mutableMapOf<String, CoachComposition>()
            allCoaches.forEach { coachName ->
                val result = trainRepository.getCoachComposition(
                    CoachCompositionRequest(
                        boardingStation = boardingStation,
                        cls = "",
                        coach = coachName,
                        jDate = journeyDate,
                        remoteStation = boardingStation,
                        trainNo = trainNumber,
                        trainSourceStation = boardingStation
                    )
                )
                if (result is Result.Success) allCoachCompositions[coachName] = result.data
            }

            // Persist everything as a single cache entry
            val entity = VacartCacheEntity(
                cacheKey = "${trainNumber}_${journeyDate}",
                trainNumber = trainNumber,
                journeyDate = journeyDate,
                trainCompositionJson = gson.toJson(composition),
                vacantBerthJson = gson.toJson(allVacantBerths),
                coachCompositionJson = gson.toJson(allCoachCompositions),
                boardingStation = boardingStation,
                vacantBerthCachedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            trainRepository.saveVacartCache(entity)
        }
    }

    private fun updateDateString(date: String) {
        _state.value = _state.value.copy(selectedDateString = date)
    }

    private fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    private fun sortByColumn(column: String) {
        val currentOrder = if (_state.value.lastSortedColumn == column) _state.value.isAscending else true
        val sortedList = when (column) {
            "fromStation" -> if (currentOrder) _state.value.vacantBerth?.vbd?.sortedBy { it.from } else _state.value.vacantBerth?.vbd?.sortedByDescending { it.from }
            "toStation" -> if (currentOrder) _state.value.vacantBerth?.vbd?.sortedBy { it.to } else _state.value.vacantBerth?.vbd?.sortedByDescending { it.to }
            "coach" -> if (currentOrder) _state.value.vacantBerth?.vbd?.sortedBy { it.coachName } else _state.value.vacantBerth?.vbd?.sortedByDescending { it.coachName }
            else -> _state.value.vacantBerth?.vbd
        }

        _state.value = _state.value.copy(
            vacantBerthList = sortedList ?: emptyList(),
            isAscending = !currentOrder,
            lastSortedColumn = column
        )
    }

    private fun selectClassCode(classCode: String) {
        _state.value = _state.value.copy(selectedClassCode = classCode)
    }

    private fun getVacantBerth() {
        viewModelScope.launch {
            val vacantBerthRequest = VacantBerthRequest(
                boardingStation = _state.value.boardingStation,
                chartType = 1,
                cls = _state.value.selectedClassCode,
                jDate = _state.value.journeyDate,
                remoteStation = _state.value.boardingStation,
                trainNo = _state.value.trainNumber,
                trainSourceStation = _state.value.boardingStation
            )
            _state.value = state.value.copy(isLoading = true)

            when (val apiResult = trainRepository.getVacantBerth(vacantBerthRequest)) {
                is Result.Success -> {
                    _state.value = state.value.copy(
                        vacantBerth = apiResult.data,
                        vacantBerthList = apiResult.data.vbd,
                        isLoading = false,
                        isOfflineData = false,
                        isStaleData = false,
                        offlineCachedAt = null
                    )
                }
                is Result.Error -> {
                    // Offline fallback: try to serve cached VacantBerth for this class
                    val cacheKey = "${_state.value.trainNumber}_${_state.value.journeyDate}"
                    val cached = trainRepository.getCachedVacantBerth(cacheKey, _state.value.selectedClassCode)
                    if (cached != null) {
                        _state.value = state.value.copy(
                            vacantBerth = cached.data,
                            vacantBerthList = cached.data.vbd,
                            isLoading = false,
                            isOfflineData = true,
                            isStaleData = cached.isStale,
                            offlineCachedAt = cached.cachedAt
                        )
                    } else {
                        _state.value = state.value.copy(showError = true, isLoading = false)
                    }
                }
                else -> {}
            }
        }
    }

    private fun selectCoach(coach: String) {
        _state.value = _state.value.copy(selectedCoach = coach)
    }

    private fun getCoachComposition() {
        viewModelScope.launch {
            val coachCompositionRequest = CoachCompositionRequest(
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
                    _state.value = state.value.copy(
                        coachComposition = apiResult.data,
                        isLoading = false,
                        isOfflineData = false,
                        isStaleData = false,
                        offlineCachedAt = null
                    )
                }
                is Result.Error -> {
                    // Offline fallback: try to serve cached CoachComposition for this coach
                    val cacheKey = "${_state.value.trainNumber}_${_state.value.journeyDate}"
                    val cached = trainRepository.getCachedCoachComposition(cacheKey, _state.value.selectedCoach)
                    if (cached != null) {
                        _state.value = state.value.copy(
                            coachComposition = cached.data,
                            isLoading = false,
                            isOfflineData = true,
                            isStaleData = cached.isStale,
                            offlineCachedAt = cached.cachedAt
                        )
                    } else {
                        _state.value = state.value.copy(showError = true, isLoading = false)
                    }
                }
                else -> {}
            }
        }
    }

    fun getStationName(stationCode: String): String {
        return state.value.stationList?.stationList?.find { station ->
            station.stationCode == stationCode
        }?.stationName ?: stationCode
    }
}