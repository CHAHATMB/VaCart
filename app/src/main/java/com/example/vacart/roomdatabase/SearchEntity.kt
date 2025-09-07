package com.example.vacart.roomdatabase

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "recent_searches")
data class SearchEntity(
    @PrimaryKey
    @Embedded
    val searchEntityKey:SearchEntityKey,
    val trainNumber: String,
    val journeyDate: String,

    val addedAt: Date = Date()
)

data class SearchEntityKey(
    @ColumnInfo(name="tn") val trainNumber: String,
    @ColumnInfo(name="jd") val journeyDate: String
)

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}