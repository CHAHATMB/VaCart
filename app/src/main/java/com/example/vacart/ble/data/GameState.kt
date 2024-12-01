package com.example.vacart.ble.data

sealed interface GameState

data class WaitingForPlayers(val connectedPlayers: Int): GameState

data object DownloadingQuestions: GameState

data class Round(
    val question: Question
): GameState
