package com.vacart.roomdatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `vacart_cache` (
                `cacheKey` TEXT NOT NULL,
                `trainNumber` TEXT NOT NULL,
                `journeyDate` TEXT NOT NULL,
                `trainCompositionJson` TEXT NOT NULL,
                `vacantBerthJson` TEXT NOT NULL,
                `coachCompositionJson` TEXT NOT NULL,
                `boardingStation` TEXT NOT NULL,
                `vacantBerthCachedAt` INTEGER NOT NULL,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`cacheKey`)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pnr_cache` (
                `pnrNumber` TEXT NOT NULL,
                `pnrResponseJson` TEXT NOT NULL,
                `cacheType` TEXT NOT NULL,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`pnrNumber`)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pnr_recent_searches` (
                `pnrNumber` TEXT NOT NULL,
                `trainName` TEXT NOT NULL,
                `trainNumber` TEXT NOT NULL,
                `journeyDate` TEXT NOT NULL,
                `sourceStation` TEXT NOT NULL,
                `destinationStation` TEXT NOT NULL,
                `isSaved` INTEGER NOT NULL DEFAULT 0,
                `cacheType` TEXT,
                `searchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`pnrNumber`)
            )
            """.trimIndent()
        )
    }
}
