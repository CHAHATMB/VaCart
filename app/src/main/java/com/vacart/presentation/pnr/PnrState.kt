package com.vacart.presentation.pnr

import android.graphics.Bitmap
import com.vacart.model.PnrResponse
import com.vacart.roomdatabase.PnrRecentSearch

data class PnrState(
    val pnrInput: String = "",
    val captchaInput: String = "",
    val captchaBitmap: Bitmap? = null,
    val pnrResponse: PnrResponse? = null,
    val isLoadingCaptcha: Boolean = false,
    val isLoadingResult: Boolean = false,
    val isRefreshingResult: Boolean = false,
    val errorMessage: String? = null,
    val step: PnrStep = PnrStep.INPUT,

    // Offline / saved state
    /** True when showing auto-cached data (offline, not a live API result). */
    val isOfflineData: Boolean = false,
    /** True when showing permanently saved data (user-saved, no TTL). */
    val isSavedData: Boolean = false,
    /** Epoch millis of when the cached data was originally fetched — for "cached X ago" display. */
    val offlineCachedAt: Long? = null,
    /** True when auto-cached data is older than its 30-min TTL — shown as a stale warning. */
    val isStaleOfflineData: Boolean = false,

    // Save-for-offline button
    /** True when ALL passengers have real coach + berth — shows "Save for offline" button. */
    val canSavePnr: Boolean = false,
    /** True while the save operation is in progress. */
    val isSaving: Boolean = false,

    // Recent PNR searches
    val recentSearches: List<PnrRecentSearch> = emptyList()
)

enum class PnrStep {
    INPUT,       // User types PNR
    CAPTCHA,     // Show captcha to user
    RESULT       // Show PNR details
}
