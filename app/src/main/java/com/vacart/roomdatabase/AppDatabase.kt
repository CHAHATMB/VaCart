package com.vacart.roomdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SearchEntity::class,
        VacartCacheEntity::class,
        PnrCacheEntity::class,
        PnrRecentSearch::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao
    abstract fun vacartCacheDao(): VacartCacheDao
    abstract fun pnrCacheDao(): PnrCacheDao
    abstract fun pnrRecentSearchDao(): PnrRecentSearchDao
}