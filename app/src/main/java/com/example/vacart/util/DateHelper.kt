package com.example.vacart.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
    fun getDateBasedOnOffset(offsetDays: Int): String {
        // Get the current date
        val currentDate = LocalDate.now()

        // Calculate the new date by adding/subtracting offsetDays
        val resultDate = currentDate.plusDays(offsetDays.toLong())

        // Format the date as "YYYY-MM-DD"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return resultDate.format(formatter)
    }
