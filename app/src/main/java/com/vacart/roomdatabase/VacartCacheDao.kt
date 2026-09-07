package com.vacart.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VacartCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VacartCacheEntity)

    @Query("SELECT * FROM vacart_cache WHERE cacheKey = :key")
    suspend fun getByKey(key: String): VacartCacheEntity?

    @Query("SELECT * FROM vacart_cache ORDER BY cachedAt DESC LIMIT 20")
    fun getAllCached(): Flow<List<VacartCacheEntity>>

    /** Evict entries whose overall cachedAt is older than [expiryTimestamp]. */
    @Query("DELETE FROM vacart_cache WHERE cachedAt < :expiryTimestamp")
    suspend fun evictExpired(expiryTimestamp: Long)
}
