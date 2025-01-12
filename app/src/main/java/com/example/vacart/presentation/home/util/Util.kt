package com.example.vacart.presentation.home.util

fun getSeatName(number: Int, classCode: String = "3A"): String {
    return if(classCode.is1A()){
        when(number%4){
            1, 3 -> "L"
            2, 0 -> "U"
            else -> ""
        }
    } else if(classCode.is2A()){
        when(number%6){
            1, 3 -> "L"
            2, 4 -> "U"
            5 -> "SL"
            0 -> "SU"
            else -> ""
        }
    } else {
        when(number%8){
            1, 4 -> "L"
            2, 5 -> "M"
            3, 6 -> "U"
            7 -> "SL"
            0 -> "SU"
            else -> ""
        }
    }

}

fun String.is1A(): Boolean {
    return this == "1A"
}

fun String.is2A(): Boolean {
    return this == "2A"
}