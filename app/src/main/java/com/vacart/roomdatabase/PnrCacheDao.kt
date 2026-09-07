package com.vacart.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PnrCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PnrCacheEntity)

    @Query("SELECT * FROM pnr_cache WHERE pnrNumber = :pnr")
    suspend fun getByPnr(pnr: String): PnrCacheEntity?

    /**
     * Evict only AUTO_30MIN entries older than [expiryTimestamp].
     * SAVED_PERMANENT entries are never evicted by the system.
     */
    @Query("DELETE FROM pnr_cache WHERE cacheType = '${PnrCacheEntity.TYPE_AUTO}' AND cachedAt < :expiryTimestamp")
    suspend fun evictExpiredAuto(expiryTimestamp: Long)

    /** Called when user explicitly deletes a saved PNR from the recent list. */
    @Query("DELETE FROM pnr_cache WHERE pnrNumber = :pnr")
    suspend fun deleteByPnr(pnr: String)
}
