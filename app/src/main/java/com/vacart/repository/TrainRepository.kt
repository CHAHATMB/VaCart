package com.vacart.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vacart.api.TrainAPI
import com.vacart.model.CoachComposition
import com.vacart.model.CoachCompositionRequest
import com.vacart.model.StationList
import com.vacart.model.TrainComposition
import com.vacart.model.TrainInfoRequest
import com.vacart.model.VacantBerth
import com.vacart.model.VacantBerthRequest
import com.vacart.roomdatabase.VacartCacheDao
import com.vacart.roomdatabase.VacartCacheEntity
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TrainRepository @Inject constructor(
    private val trainAPI: TrainAPI,
    private val vacartCacheDao: VacartCacheDao
) {
    private val gson = Gson()

    // ---------------------------------------------------------------------------
    // TTL constants
    // ---------------------------------------------------------------------------
    private val COMPOSITION_TTL_MS = TimeUnit.HOURS.toMillis(12)
    private val VACANT_BERTH_TTL_MS = TimeUnit.MINUTES.toMillis(30)

    // ---------------------------------------------------------------------------
    // Public API calls (network-first; no cache read here — used during prefetch)
    // ---------------------------------------------------------------------------

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

    /**
     * Fetches vacant berths. On success the caller is responsible for saving to cache
     * (via [saveVacartCache]) so that all class codes are bundled into one entity.
     */
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

    // ---------------------------------------------------------------------------
    // Cache read helpers — called as offline fallback in HomeViewModel
    // ---------------------------------------------------------------------------

    /**
     * Returns the cached [TrainComposition] for the given key if it exists and is not expired.
     * Returns stale data (with [isStale]=true) if the TTL has passed but data exists.
     */
    suspend fun getCachedTrainComposition(cacheKey: String): CachedResult<TrainComposition>? {
        val entity = vacartCacheDao.getByKey(cacheKey) ?: return null
        val composition = gson.fromJson(entity.trainCompositionJson, TrainComposition::class.java)
        val age = System.currentTimeMillis() - entity.cachedAt
        return CachedResult(composition, cachedAt = entity.cachedAt, isStale = age > COMPOSITION_TTL_MS)
    }

    /**
     * Returns the cached [VacantBerth] for a specific [classCode] from the cache entry.
     * VacantBerth has its own shorter TTL ([VACANT_BERTH_TTL_MS]).
     */
    suspend fun getCachedVacantBerth(cacheKey: String, classCode: String): CachedResult<VacantBerth>? {
        val entity = vacartCacheDao.getByKey(cacheKey) ?: return null
        val type = object : TypeToken<Map<String, VacantBerth>>() {}.type
        val map: Map<String, VacantBerth> = gson.fromJson(entity.vacantBerthJson, type) ?: return null
        val berth = map[classCode] ?: return null
        val age = System.currentTimeMillis() - entity.vacantBerthCachedAt
        return CachedResult(berth, cachedAt = entity.vacantBerthCachedAt, isStale = age > VACANT_BERTH_TTL_MS)
    }

    /**
     * Returns the cached [CoachComposition] for a specific [coachName] from the cache entry.
     */
    suspend fun getCachedCoachComposition(cacheKey: String, coachName: String): CachedResult<CoachComposition>? {
        val entity = vacartCacheDao.getByKey(cacheKey) ?: return null
        val type = object : TypeToken<Map<String, CoachComposition>>() {}.type
        val map: Map<String, CoachComposition> = gson.fromJson(entity.coachCompositionJson, type) ?: return null
        val coach = map[coachName] ?: return null
        val age = System.currentTimeMillis() - entity.cachedAt
        return CachedResult(coach, cachedAt = entity.cachedAt, isStale = age > COMPOSITION_TTL_MS)
    }

    // ---------------------------------------------------------------------------
    // Cache write helper
    // ---------------------------------------------------------------------------

    suspend fun saveVacartCache(entity: VacartCacheEntity) {
        vacartCacheDao.upsert(entity)
    }

    suspend fun evictExpiredVacartCache() {
        val expiry = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        vacartCacheDao.evictExpired(expiry)
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

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

/** Wraps a cached value together with its age metadata. */
data class CachedResult<T>(
    val data: T,
    val cachedAt: Long,
    val isStale: Boolean
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    /** Returned when the server explicitly rejects the captcha answer. */
    data class CaptchaError(val message: String) : Result<Nothing>()
    /** Returned when the server session has expired or the request is invalid. */
    object SessionError : Result<Nothing>()
    /** Returned when the PNR is flushed or not yet generated. */
    data class InvalidPnr(val reason: String) : Result<Nothing>()
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
