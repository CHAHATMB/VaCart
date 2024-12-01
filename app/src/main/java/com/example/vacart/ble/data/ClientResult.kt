package com.example.vacart.ble.data

/**
 * To maps players name with device.
 * @property name           name of the player.
 * @property deviceAddress  bluetooth device address from where name was sent.
 */
data class Name(
    val name: String,
    val deviceAddress: String,
)