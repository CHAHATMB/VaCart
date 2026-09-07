package com.vacart.model

data class TrainInfo(
    val trainNumber: String,
    val trainName: String,
    val rawDisplay: String,
    val searchableNumber: String,
    val searchableName: String
) {
    companion object {
        fun fromRaw(raw: String): TrainInfo {
            val cleaned = raw.trim().trim('"').trim('[', ']')
            val parts = cleaned.split(" - ", limit = 2)
            val number = if (parts.isNotEmpty()) parts[0].trim() else ""
            val name = if (parts.size > 1) parts[1].trim() else ""
            return TrainInfo(
                trainNumber = number,
                trainName = name,
                rawDisplay = if (name.isNotEmpty()) "$number - $name" else number,
                searchableNumber = number.lowercase(),
                searchableName = name.lowercase()
            )
        }
    }
}
