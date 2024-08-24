package com.example.vacart.ble.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vacart.ble.data.ClientViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.ktx.stateAsFlow
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val scannerRepository: ScannerRepository,
) : ViewModel()  {
    private var clientManager: ClientConnection? = null
    private val _clientState: MutableStateFlow<ClientViewState> =
        MutableStateFlow(ClientViewState())
    val clientState = _clientState.asStateFlow()

    init {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("ClientViewModel", "Error", throwable)
            _clientState.value = _clientState.value.copy(error = throwable.message)
        }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val device = scannerRepository.searchForServer()

            ClientConnection(context, viewModelScope, device)
                .apply {
                    stateAsFlow()
                        .onEach { _clientState.value = _clientState.value.copy(state = it) }
                        .launchIn(viewModelScope)
                    error
                        .onEach { _clientState.value = _clientState.value.copy(nameResult = it) }
                        .launchIn(viewModelScope)
                     userJoined
                        .onEach { _clientState.value = _clientState.value.copy(userJoined = it) }
                        .launchIn(viewModelScope)
                    question
                        .onEach {
                            _clientState.value = _clientState.value.copy(
                                selectedAnswerId = null,
                                correctAnswerId = null,
                                ticks = 0L,
                                question = it
                            )
                        }
                        .launchIn(viewModelScope)
                    answer
                        .onEach {
                            _clientState.value = _clientState.value.copy(
                                correctAnswerId = it,
                                ticks = 0L
                            )
                        }
                        .launchIn(viewModelScope)
                    isGameOver
                        .onEach { _clientState.value = _clientState.value.copy(isGameOver = it) }
                        .launchIn(viewModelScope)
                    result
                        .onEach { _clientState.value = _clientState.value.copy(result = it) }
                        .launchIn(viewModelScope)
                }
                .apply { connect() }
                .apply { clientManager = this }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clientManager?.release()
        clientManager = null
    }

    fun sendAnswer(answerId: Int) {
        _clientState.value = _clientState.value.copy(
            selectedAnswerId = answerId,
            ticks = 0L
        )

        viewModelScope.launch(Dispatchers.IO) {
            clientManager?.sendSelectedAnswer(answerId)
        }
    }

    fun onUserTyping() {
        _clientState.value = _clientState.value.copy(isUserTyping = true)
    }

    fun dismissPlayersNameDialog() {
        _clientState.value = _clientState.value.copy(userRequestedPlayersNameDialog = false)
    }

    fun sendName(playersName: String) {
        _clientState.value = _clientState.value.copy(isUserTyping = false, nameResult = null)
        viewModelScope.launch(Dispatchers.IO) {
            clientManager?.sendPlayersName(playersName)
        }
    }
}