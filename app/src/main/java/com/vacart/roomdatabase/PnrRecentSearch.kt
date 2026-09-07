package com.vacart.roomdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a user's recent PNR lookups for the "Recent PNRs" list on the PNR screen.
 *
 * isSaved      : true when the user explicitly tapped "Save for offline" — pinned to top of list.
 * cacheType    : mirrors PnrCacheEntity.cacheType for badge display in the recent list UI.
 *                null means the PNR was looked up but is not cached (WL + chart not prepared).
 */
@Entity(tableName = "pnr_recent_searches")
data class PnrRecentSearch(
    @PrimaryKey
    val pnrNumber: String,
    val trainName: String,
    val trainNumber: String,
    val journeyDate: String,
    val sourceStation: String,
    val destinationStation: String,
    val isSaved: Boolean = false,
    val cacheType: String? = null,
    val searchedAt: Long = System.currentTimeMillis()
)
