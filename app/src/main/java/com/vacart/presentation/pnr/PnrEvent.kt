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
}
