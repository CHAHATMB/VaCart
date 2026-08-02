package com.vacart.repository

import com.vacart.api.TrainAPI
import com.vacart.model.CoachComposition
import com.vacart.model.CoachCompositionRequest
import com.vacart.model.StationList
import com.vacart.model.TrainComposition
import com.vacart.model.TrainInfoRequest
import com.vacart.model.VacantBerth
import com.vacart.model.VacantBerthRequest
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

