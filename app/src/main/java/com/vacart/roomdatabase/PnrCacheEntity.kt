package com.vacart.roomdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a cached PNR API response.
 *
 * cacheType:
 *  - "AUTO_30MIN"       : auto-cached after a successful fetch (chart prepared or berth allocated).
 *                         Evicted after 30 minutes by the background cleanup job.
 *  - "SAVED_PERMANENT"  : user explicitly saved the PNR for offline reference.
 *                         Never auto-evicted — only removed when the user deletes it.
 *
 * WL tickets where the chart is NOT yet prepared are never written to this table.
 */
@Entity(tableName = "pnr_cache")
data class PnrCacheEntity(
    @PrimaryKey
    val pnrNumber: String,
    val pnrResponseJson: String,
    val cacheType: String,          // "AUTO_30MIN" | "SAVED_PERMANENT"
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_AUTO = "AUTO_30MIN"
        const val TYPE_SAVED = "SAVED_PERMANENT"
    }
}
