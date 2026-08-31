package com.vacart.presentation.pnr

import android.graphics.Bitmap
import com.vacart.model.PnrResponse

data class PnrState(
    val pnrInput: String = "",
    val captchaInput: String = "",
    val captchaBitmap: Bitmap? = null,
    val pnrResponse: PnrResponse? = null,
    val isLoadingCaptcha: Boolean = false,
    val isLoadingResult: Boolean = false,
    val isRefreshingResult: Boolean = false,
    val errorMessage: String? = null,
    val step: PnrStep = PnrStep.INPUT
)

enum class PnrStep {
    INPUT,       // User types PNR
    CAPTCHA,     // Show captcha to user
    RESULT       // Show PNR details
}
