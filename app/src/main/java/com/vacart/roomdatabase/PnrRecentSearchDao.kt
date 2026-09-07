package com.vacart.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PnrRecentSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PnrRecentSearch)

    /**
     * Return recent PNR searches sorted by:
     * 1. Saved (pinned) items first
     * 2. Then by recency
     */
    @Query("SELECT * FROM pnr_recent_searches ORDER BY isSaved DESC, searchedAt DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<PnrRecentSearch>>

    @Query("DELETE FROM pnr_recent_searches WHERE pnrNumber = :pnr")
    suspend fun delete(pnr: String)

    /** Promote an existing entry to permanently saved state. */
    @Query("UPDATE pnr_recent_searches SET isSaved = 1, cacheType = '${PnrCacheEntity.TYPE_SAVED}' WHERE pnrNumber = :pnr")
    suspend fun markAsSaved(pnr: String)
}
