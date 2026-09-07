package com.vacart.util

import com.vacart.model.PnrResponse
import com.vacart.roomdatabase.PnrCacheEntity

/**
 * Encapsulates all caching-eligibility decisions for PNR responses.
 *
 * Key insight: A passenger with currentStatus="CNF" but no coach/berth assigned is a
 * WL ticket that got a tentative "CNF" label before chart preparation — it cannot be
 * treated as fully confirmed for caching purposes because the status can still change.
 *
 * Caching rules:
 *  - WL ticket + chart NOT prepared → DO_NOT_CACHE (status changes frequently)
 *  - Chart prepared OR any passenger has a real berth assigned → AUTO_CACHE_30MIN
 *  - All passengers have real berths → canUserSave = true (show "Save" button)
 */
object PnrCachePolicy {

    private const val THIRTY_MINUTES_MS = 30 * 60 * 1000L

    /**
     * Returns true if at least one passenger has an actual coach + berth allocated.
     * Both fields must be non-empty/non-zero — a status string alone is unreliable.
     */
    fun hasBerthAllocated(response: PnrResponse): Boolean =
        response.passengerList.any { p ->
            p.currentCoachId.isNotBlank() && p.currentBerthNo > 0
        }

    /**
     * Returns true if the chart has been prepared for this train.
     * IRCTC returns strings like "CHART PREPARED", "FINAL CHART PREPARED".
     */
    fun isChartPrepared(response: PnrResponse): Boolean =
        response.chartStatus.contains("CHART PREPARED", ignoreCase = true)

    /**
     * Determines whether this PNR response should be auto-cached and with which type.
     */
    fun resolveCacheType(response: PnrResponse): String? {
        val chartPrepared = isChartPrepared(response)
        val berthAllocated = hasBerthAllocated(response)
        val isWaitingList = response.isWL == "Y"

        return when {
            // WL + chart not ready → statuses will keep changing
            isWaitingList && !chartPrepared -> null
            // Chart prepared or any berth allocated → stable enough to cache for 30 min
            chartPrepared || berthAllocated -> PnrCacheEntity.TYPE_AUTO
            // Looks confirmed but chart not prepared yet — don't cache
            else -> null
        }
    }

    /**
     * Returns true if the user should see the "Save for offline" button.
     * Condition: ALL passengers must have a real coach + berth assigned.
     */
    fun canUserSave(response: PnrResponse): Boolean =
        response.passengerList.isNotEmpty() &&
            response.passengerList.all { p ->
                p.currentCoachId.isNotBlank() && p.currentBerthNo > 0
            }

    /**
     * Returns true if a cached entry with [cachedAt] timestamp is still within the 30-min TTL.
     * Always returns true for SAVED_PERMANENT entries (they never expire).
     */
    fun isCacheValid(cacheType: String, cachedAt: Long): Boolean {
        if (cacheType == PnrCacheEntity.TYPE_SAVED) return true
        return (System.currentTimeMillis() - cachedAt) < THIRTY_MINUTES_MS
    }
}
