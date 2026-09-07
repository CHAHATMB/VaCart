package com.vacart.presentation.pnr

sealed class PnrEvent {
    data class UpdatePnrInput(val pnr: String) : PnrEvent()
    data class UpdateCaptchaInput(val captcha: String) : PnrEvent()
    object FetchCaptcha : PnrEvent()
    object SubmitCaptcha : PnrEvent()
    object RefreshCaptcha : PnrEvent()
    object Reset : PnrEvent()
    /** Re-fetch PNR details from the result screen using the existing session (no captcha). */
    object RefreshPnrStatus : PnrEvent()

    /** User taps a recent PNR item — auto-loads from cache if valid, else pre-fills input. */
    data class SelectRecentPnr(val pnr: String) : PnrEvent()
    /** User deletes a recent PNR entry (removes from recent list AND cache). */
    data class DeleteRecentPnr(val pnr: String) : PnrEvent()
    /** User taps "Save for offline" on the result screen. */
    object SavePnr : PnrEvent()
}
