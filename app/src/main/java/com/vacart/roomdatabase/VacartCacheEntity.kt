package com.vacart.roomdatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches all VaCart-related API responses for a given train+date combination.
 * A single entry covers TrainComposition, all class-keyed VacantBerth responses,
 * and all coach-keyed CoachComposition responses (stored as JSON blobs).
 *
 * cacheKey  : "${trainNumber}_${journeyDate}"  e.g. "12302_20260908"
 * vacantBerthJson : Gson-serialized Map<classCode, VacantBerth>
 * coachCompositionJson : Gson-serialized Map<coachName, CoachComposition>
 */
@Entity(tableName = "vacart_cache")
data class VacartCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val trainNumber: String,
    val journeyDate: String,
    val trainCompositionJson: String,
    val vacantBerthJson: String,
    val coachCompositionJson: String,
    val boardingStation: String,
    /** Separate timestamp for vacant-berth data since it has a shorter TTL (30 min). */
    val vacantBerthCachedAt: Long = System.currentTimeMillis(),
    val cachedAt: Long = System.currentTimeMillis()
)
