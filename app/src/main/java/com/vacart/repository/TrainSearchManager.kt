package com.vacart.repository

import android.content.Context
import android.content.SharedPreferences
import com.vacart.api.TrainAPI
import com.vacart.model.TrainInfo
import com.vacart.util.trainList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainSearchManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trainAPI: TrainAPI
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("train_search_prefs", Context.MODE_PRIVATE)
    }

    private var cachedTrainList: List<TrainInfo>? = null

    suspend fun getTrainList(): List<TrainInfo> = withContext(Dispatchers.IO) {
        cachedTrainList?.let { return@withContext it }

        val cacheFile = File(context.filesDir, "train_list_cache.txt")
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        val currentTime = System.currentTimeMillis()
        val isCacheExpired = (currentTime - lastUpdated) > CACHE_DURATION_MS

        var rawContent: String? = null

        // 1. Read from valid disk cache if available
        if (!isCacheExpired && cacheFile.exists()) {
            try {
                rawContent = cacheFile.readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fetch from network if cache expired or missing
        if (rawContent.isNullOrBlank()) {
            try {
                val response = trainAPI.getTrainList()
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    if (!bodyString.isNullOrBlank()) {
                        rawContent = bodyString
                        cacheFile.writeText(bodyString)
                        prefs.edit().putLong(KEY_LAST_UPDATED, currentTime).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Fallback to expired cache file if network request failed
        if (rawContent.isNullOrBlank() && cacheFile.exists()) {
            try {
                rawContent = cacheFile.readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Parse content or fallback to static trainList array
        val parsedList = if (!rawContent.isNullOrBlank()) {
            parseRawTrainString(rawContent)
        } else {
            trainList.map { TrainInfo.fromRaw(it) }
        }

        cachedTrainList = parsedList
        return@withContext parsedList
    }

    fun searchTrains(allTrains: List<TrainInfo>, query: String, limit: Int = 20): List<TrainInfo> {
        val trimmed = query.trim().lowercase()
        if (trimmed.length < 2) return emptyList()

        return allTrains.asSequence()
            .filter { train ->
                train.searchableNumber.contains(trimmed) || train.searchableName.contains(trimmed)
            }
            .sortedWith { a, b ->
                when {
                    a.searchableNumber == trimmed -> -1
                    b.searchableNumber == trimmed -> 1
                    a.searchableNumber.startsWith(trimmed) && !b.searchableNumber.startsWith(trimmed) -> -1
                    !a.searchableNumber.startsWith(trimmed) && b.searchableNumber.startsWith(trimmed) -> 1
                    a.searchableName.startsWith(trimmed) && !b.searchableName.startsWith(trimmed) -> -1
                    !a.searchableName.startsWith(trimmed) && b.searchableName.startsWith(trimmed) -> 1
                    else -> a.searchableNumber.compareTo(b.searchableNumber)
                }
            }
            .take(limit)
            .toList()
    }

    private fun parseRawTrainString(raw: String): List<TrainInfo> {
        return raw.split(",")
            .mapNotNull { item ->
                val cleaned = item.trim().trim('"').trim('[', ']')
                if (cleaned.isNotBlank()) TrainInfo.fromRaw(cleaned) else null
            }
    }

    companion object {
        private const val KEY_LAST_UPDATED = "last_updated_time"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
