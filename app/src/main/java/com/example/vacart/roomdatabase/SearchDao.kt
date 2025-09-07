package com.example.vacart.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY addedAt DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchEntity>>

    @Query("DELETE FROM recent_searches WHERE trainNumber = :trainNumber AND journeyDate = :journeyDate")
    suspend fun deleteSearch(trainNumber: String, journeyDate: String)
}
