package com.example.vacart.ble.data

data class NameResult(
    val isEmptyName: Boolean,
    val isDuplicateName: Boolean,
) {
    fun isInvalid() = isDuplicateName || isEmptyName
}

