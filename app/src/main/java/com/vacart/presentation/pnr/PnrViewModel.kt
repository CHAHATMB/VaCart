package com.vacart.presentation.pnr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.vacart.model.PnrResponse
import com.vacart.repository.PnrRepository
import com.vacart.repository.Result
import com.vacart.roomdatabase.PnrCacheDao
import com.vacart.roomdatabase.PnrCacheEntity
import com.vacart.roomdatabase.PnrRecentSearch
import com.vacart.roomdatabase.PnrRecentSearchDao
import com.vacart.util.PnrCachePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PnrViewModel @Inject constructor(
    private val pnrRepository: PnrRepository,
    private val pnrCacheDao: PnrCacheDao,
    private val pnrRecentSearchDao: PnrRecentSearchDao
) : ViewModel() {

    private val _state = MutableStateFlow(PnrState())
    val state: StateFlow<PnrState> = _state.asStateFlow()

    private val gson = Gson()

    /** Recent PNR searches — saved items pinned to top, then by recency. */
    val recentSearches = pnrRecentSearchDao.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            is PnrEvent.SelectRecentPnr -> selectRecentPnr(event.pnr)
            is PnrEvent.DeleteRecentPnr -> deleteRecentPnr(event.pnr)
            is PnrEvent.SavePnr -> savePnr()
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
                    handleSuccessfulPnrResponse(result.data)
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
                    // Try cache fallback before showing error
                    if (!tryLoadFromCache(_state.value.pnrInput)) {
                        _state.value = _state.value.copy(
                            isLoadingResult = false,
                            errorMessage = result.exception.message ?: "Something went wrong",
                            captchaInput = "",
                            captchaBitmap = null,
                            step = PnrStep.INPUT
                        )
                    }
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
                    // Update cache with refreshed data too
                    handleSuccessfulPnrResponse(result.data, isRefresh = true)
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

    // ---------------------------------------------------------------------------
    // Recent PNR: tap to auto-load or pre-fill
    // ---------------------------------------------------------------------------

    /**
     * When user taps a recent PNR:
     * - If a valid cache entry exists → skip captcha, show result directly.
     * - If no valid cache → pre-fill PNR input, let user trigger captcha normally.
     */
    private fun selectRecentPnr(pnr: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(pnrInput = pnr, isLoadingResult = true)

            val cached = pnrCacheDao.getByPnr(pnr)
            if (cached != null && PnrCachePolicy.isCacheValid(cached.cacheType, cached.cachedAt)) {
                val response = gson.fromJson(cached.pnrResponseJson, PnrResponse::class.java)
                val isPermanent = cached.cacheType == PnrCacheEntity.TYPE_SAVED
                _state.value = _state.value.copy(
                    pnrResponse = response,
                    isLoadingResult = false,
                    step = PnrStep.RESULT,
                    isOfflineData = !isPermanent,
                    isSavedData = isPermanent,
                    offlineCachedAt = if (!isPermanent) cached.cachedAt else null,
                    canSavePnr = false  // cannot save while offline
                )
            } else {
                // No valid cache — pre-fill PNR, go to INPUT for user to trigger captcha
                _state.value = _state.value.copy(
                    isLoadingResult = false,
                    step = PnrStep.INPUT
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Delete recent PNR
    // ---------------------------------------------------------------------------

    private fun deleteRecentPnr(pnr: String) {
        viewModelScope.launch {
            pnrRecentSearchDao.delete(pnr)
            pnrCacheDao.deleteByPnr(pnr)
        }
    }

    // ---------------------------------------------------------------------------
    // Save PNR permanently
    // ---------------------------------------------------------------------------

    private fun savePnr() {
        val response = _state.value.pnrResponse ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            // Upsert with SAVED_PERMANENT — overwrites any existing AUTO_30MIN entry
            pnrCacheDao.upsert(
                PnrCacheEntity(
                    pnrNumber = response.pnrNumber,
                    pnrResponseJson = gson.toJson(response),
                    cacheType = PnrCacheEntity.TYPE_SAVED
                )
            )
            // Mark the recent search entry as saved (pins it to top of list)
            pnrRecentSearchDao.markAsSaved(response.pnrNumber)

            _state.value = _state.value.copy(
                isSaving = false,
                isSavedData = true,
                isOfflineData = false,
                offlineCachedAt = null,
                canSavePnr = false  // hide Save button after saving
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Handles a fresh [PnrResponse] from the API:
     * - Conditionally auto-caches based on [PnrCachePolicy]
     * - Always upserts the recent search entry
     * - Updates UI state
     */
    private suspend fun handleSuccessfulPnrResponse(response: PnrResponse, isRefresh: Boolean = false) {
        val resolvedCacheType = PnrCachePolicy.resolveCacheType(response)
        val canSave = PnrCachePolicy.canUserSave(response)

        if (resolvedCacheType != null) {
            // Only cache if the ticket status warrants it
            pnrCacheDao.upsert(
                PnrCacheEntity(
                    pnrNumber = response.pnrNumber,
                    pnrResponseJson = gson.toJson(response),
                    cacheType = resolvedCacheType
                )
            )
        }

        // Always record in recent searches (user searched it regardless of cacheability)
        pnrRecentSearchDao.upsert(
            PnrRecentSearch(
                pnrNumber = response.pnrNumber,
                trainName = response.trainName,
                trainNumber = response.trainNumber,
                journeyDate = response.dateOfJourney,
                sourceStation = response.sourceStation,
                destinationStation = response.destinationStation,
                isSaved = false,
                cacheType = resolvedCacheType
            )
        )

        if (isRefresh) {
            _state.value = _state.value.copy(
                pnrResponse = response,
                isRefreshingResult = false,
                isOfflineData = false,
                isSavedData = false,
                offlineCachedAt = null,
                canSavePnr = canSave
            )
        } else {
            _state.value = _state.value.copy(
                pnrResponse = response,
                isLoadingResult = false,
                step = PnrStep.RESULT,
                isOfflineData = false,
                isSavedData = false,
                offlineCachedAt = null,
                canSavePnr = canSave
            )
        }
    }

    /**
     * Attempts to load a cached PNR response and update state.
     * Returns true if a usable (valid or stale) cache entry was found and displayed.
     */
    private suspend fun tryLoadFromCache(pnr: String): Boolean {
        val cached = pnrCacheDao.getByPnr(pnr) ?: return false
        val response = gson.fromJson(cached.pnrResponseJson, PnrResponse::class.java)
        val isPermanent = cached.cacheType == PnrCacheEntity.TYPE_SAVED
        val isWithinTtl = PnrCachePolicy.isCacheValid(cached.cacheType, cached.cachedAt)

        // Serve either valid cache or stale cache (with a warning) — both are better than nothing
        _state.value = _state.value.copy(
            pnrResponse = response,
            isLoadingResult = false,
            step = PnrStep.RESULT,
            isOfflineData = !isPermanent,
            isSavedData = isPermanent,
            offlineCachedAt = if (!isPermanent) cached.cachedAt else null,
            // Stale AUTO cache: flag as stale so the banner shows a warning
            isStaleOfflineData = !isPermanent && !isWithinTtl,
            canSavePnr = false  // can't save while offline
        )
        return true
    }
}
