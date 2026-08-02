package com.vacart.repository

import com.vacart.api.TrainAPI
import com.vacart.model.CoachComposition
import com.vacart.model.CoachCompositionRequest
import com.vacart.model.StationList
import com.vacart.model.TrainComposition
import com.vacart.model.TrainInfoRequest
import com.vacart.model.VacantBerth
import com.vacart.model.VacantBerthRequest
import org.json.JSONObject
import javax.inject.Inject

class TrainRepository @Inject constructor(private val trainAPI: TrainAPI) {

    suspend fun getStationList(trainNumber: String): Result<StationList> {
        return safeApiCall {
            val response = trainAPI.getStationList(trainNumber)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.errorMessage.isNullOrEmpty()) {
                    throw Exception(body.errorMessage)
                }
                body
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                throw Exception(errorMsg ?: "Currently services are not available due to daily maintenance downtime.")
            }
        }
    }

    suspend fun getTrainComposition(trainInfoRequest: TrainInfoRequest): Result<TrainComposition> {
        return safeApiCall {
            val response = trainAPI.getTrainComposition(trainInfoRequest)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.errorMessage.isNullOrEmpty()) {
                    throw Exception(body.errorMessage)
                }
                body
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                throw Exception(errorMsg ?: "Currently services are not available due to daily maintenance downtime.")
            }
        }
    }

    suspend fun getVacantBerth(vacantBerthRequest: VacantBerthRequest): Result<VacantBerth> {
        return safeApiCall {
            val response = trainAPI.getVacantBerth(vacantBerthRequest)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                throw Exception(errorMsg ?: "Failed to fetch vacant berths")
            }
        }
    }

    suspend fun getCoachComposition(coachCompositionRequest: CoachCompositionRequest): Result<CoachComposition> {
        return safeApiCall {
            val response = trainAPI.getCoachComposition(coachCompositionRequest)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                throw Exception(errorMsg ?: "Failed to fetch coach composition")
            }
        }
    }

    private fun parseErrorMessage(jsonString: String?): String? {
        if (jsonString.isNullOrEmpty()) return null
        return try {
            val jsonObject = JSONObject(jsonString)
            if (jsonObject.has("errorMessage") && !jsonObject.isNull("errorMessage")) {
                jsonObject.getString("errorMessage")
            } else null
        } catch (e: Exception) {
            null
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
    } catch (e: Exception) {
        Result.Error(e)
    }
}
