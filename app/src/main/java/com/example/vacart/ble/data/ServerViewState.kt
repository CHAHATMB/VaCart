package com.example.vacart.ble.data

/**
 * It holds the data to be used in the server screen.
 * @property state              game state.
 * @property correctAnswerId    correct answer id.
 * @property selectedAnswerId   selected answer id.
 * @property ticks              timer duration.
 * @property userJoined         a list of all joined players.
 * @property isGameOver         returns true when game is over.
 * @property result             a list of players and their scores.
 * @property isTimerRunning     returns true when timer is running.
 */

data class ServerViewState(
    val state: GameState = WaitingForPlayers(0),
    val correctAnswerId: Int? = null,
    val selectedAnswerId: Int? = null,
    val ticks: Long? = null,
    val userJoined: List<Player> = emptyList(),
    val isGameOver: Boolean? = null,
    val result: List<Result> = emptyList(),
) {
    val isTimerRunning: Boolean = ticks?.let { it > 0 } == true
    val isAllNameCollected: Boolean = userJoined.size == (when(val currentState = state) {
        is WaitingForPlayers ->
            currentState.connectedPlayers  + 1
        else -> {false}
    })

    fun isDuplicate(playerName: String): Boolean =
        (userJoined.find { it.name == playerName }?.name == playerName)
}

