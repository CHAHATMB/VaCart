package com.vacart.util

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

    fun getFormattedDateForNtes(offsetDays: Int): String {
        val resultDate = LocalDate.now().plusDays(offsetDays.toLong())
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH)
        return resultDate.format(formatter)
    }

