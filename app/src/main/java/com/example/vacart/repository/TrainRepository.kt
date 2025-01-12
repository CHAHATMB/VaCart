package com.example.vacart.repository

import com.example.vacart.api.TrainAPI
import com.example.vacart.model.CoachComposition
import com.example.vacart.model.CoachCompositionRequest
import com.example.vacart.model.StationList
import com.example.vacart.model.TrainComposition
import com.example.vacart.model.TrainInfoRequest
import com.example.vacart.model.VacantBerth
import com.example.vacart.model.VacantBerthRequest
import javax.inject.Inject

class TrainRepository @Inject constructor(private val trainAPI: TrainAPI) {

    suspend fun getStationList(trainNumber: String): Result<StationList> {
        return safeApiCall {
            trainAPI.getStationList(trainNumber).body()!!
        }
    }

    suspend fun getTrainComposition(trainInfoRequest: TrainInfoRequest): Result<TrainComposition> {
        return safeApiCall {
            trainAPI.getTrainComposition(trainInfoRequest).body()!!
        }
    }

    suspend fun getVacantBerth(vacantBerthRequest: VacantBerthRequest): Result<VacantBerth> {
        return safeApiCall {
            trainAPI.getVacantBerth(vacantBerthRequest).body()!!
        }
    }

    suspend fun getCoachComposition(coachCompositionRequest: CoachCompositionRequest): Result<CoachComposition> {
        return safeApiCall {
            trainAPI.getCoachComposition(coachCompositionRequest).body()!!
        }
    }
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        val response = apiCall()
        Result.Success(response)
//        Result.success(response)
    } catch (e: Exception) {
        Result.Error(e)
//        Result.failure(e)
    }
}

