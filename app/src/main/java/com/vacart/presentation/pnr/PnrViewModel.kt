package com.vacart.presentation.pnr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vacart.repository.PnrRepository
import com.vacart.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PnrViewModel @Inject constructor(
    private val pnrRepository: PnrRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PnrState())
    val state: StateFlow<PnrState> = _state.asStateFlow()

    fun onEvent(event: PnrEvent) {
        when (event) {
            is PnrEvent.UpdatePnrInput -> {
                _state.value = _state.value.copy(pnrInput = event.pnr, errorMessage = null)
            }
            is PnrEvent.UpdateCaptchaInput -> {
                _state.value = _state.value.copy(captchaInput = event.captcha, errorMessage = null)
            }
            is PnrEvent.FetchCaptcha -> fetchCaptcha()
            is PnrEvent.RefreshCaptcha -> fetchCaptcha()
            is PnrEvent.SubmitCaptcha -> submitCaptcha()
            is PnrEvent.RefreshPnrStatus -> refreshPnrStatus()
            is PnrEvent.Reset -> {
                _state.value = PnrState()
            }
        }
    }

    private fun fetchCaptcha() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoadingCaptcha = true,
                errorMessage = null,
                captchaBitmap = null,
                captchaInput = "",
                step = PnrStep.CAPTCHA
            )
            when (val result = pnrRepository.fetchCaptcha()) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        captchaBitmap = result.data,
                        isLoadingCaptcha = false
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingCaptcha = false,
                        errorMessage = result.exception.message ?: "Failed to load captcha",
                        step = PnrStep.INPUT
                    )
                }
                else -> {
                    _state.value = _state.value.copy(isLoadingCaptcha = false)
                }
            }
        }
    }

    private fun submitCaptcha() {
        val captcha = _state.value.captchaInput.trim()
        if (captcha.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "Please enter the captcha answer")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingResult = true, errorMessage = null)
            when (val result = pnrRepository.queryPnrStatus(_state.value.pnrInput, captcha)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        pnrResponse = result.data,
                        isLoadingResult = false,
                        step = PnrStep.RESULT
                    )
                }
                is Result.SessionError -> {
                    // Session expired — reset fully so a new session is established from scratch
                    _state.value = PnrState(
                        pnrInput = _state.value.pnrInput,
                        errorMessage = "Session expired. Please verify the captcha again."
                    )
                }
                is Result.CaptchaError -> {
                    // Wrong captcha — stay on CAPTCHA step and auto-refresh the image
                    _state.value = _state.value.copy(
                        isLoadingResult = false,
                        captchaInput = "",
                        errorMessage = "Incorrect captcha. A new one has been loaded."
                    )
                    refreshCaptchaOnly()
                }
                is Result.InvalidPnr -> {
                    // PNR is flushed or not yet generated — go back to INPUT with the error
                    _state.value = PnrState(
                        pnrInput = _state.value.pnrInput,
                        errorMessage = result.reason
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingResult = false,
                        errorMessage = result.exception.message ?: "Something went wrong",
                        captchaInput = "",
                        captchaBitmap = null,
                        step = PnrStep.INPUT
                    )
                }
                else -> {
                    _state.value = _state.value.copy(isLoadingResult = false)
                }
            }
        }
    }

    /** Refresh captcha image in-place without resetting the session or step. */
    private fun refreshCaptchaOnly() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingCaptcha = true, captchaBitmap = null)
            when (val result = pnrRepository.refreshCaptchaImage()) {
                is Result.Success -> {
                    _state.value = _state.value.copy(captchaBitmap = result.data, isLoadingCaptcha = false)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingCaptcha = false,
                        errorMessage = result.exception.message ?: "Failed to reload captcha"
                    )
                }
                else -> _state.value = _state.value.copy(isLoadingCaptcha = false)
            }
        }
    }

    private fun refreshPnrStatus() {
        val pnr = _state.value.pnrInput
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshingResult = true, errorMessage = null)
            when (val result = pnrRepository.refreshPnrStatus(pnr)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        pnrResponse = result.data,
                        isRefreshingResult = false
                    )
                }
                is Result.SessionError -> {
                    // Session expired — take user back to step one with PNR pre-filled
                    _state.value = PnrState(
                        pnrInput = pnr,
                        errorMessage = "Session expired. Please verify the captcha again."
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isRefreshingResult = false,
                        errorMessage = result.exception.message ?: "Failed to refresh status"
                    )
                }
                else -> _state.value = _state.value.copy(isRefreshingResult = false)
            }
        }
    }
}
