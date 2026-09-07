package com.vacart.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.vacart.model.PnrResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.vacart.provideHttpLoggingInterceptor

@Singleton
class PnrRepository @Inject constructor() {

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .addInterceptor(provideHttpLoggingInterceptor())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun resetSession() {
        cookieManager.cookieStore.removeAll()
    }

    suspend fun fetchCaptcha(): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            resetSession()

            // Step 1: Initialize session
            val initRequest = Request.Builder()
                .url("https://www.indianrail.gov.in/enquiry/PNR/PnrEnquiry.html?locale=en")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            httpClient.newCall(initRequest).execute().close()

            // Step 2: Fetch captcha image
            fetchCaptchaImage()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Refresh the captcha image WITHOUT resetting the session cookies.
     * Used after a captcha mismatch or manual refresh while staying on the CAPTCHA step.
     */
    suspend fun refreshCaptchaImage(): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            fetchCaptchaImage()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun fetchCaptchaImage(): Result<Bitmap> {
        val timestamp = System.currentTimeMillis()
        val captchaRequest = Request.Builder()
            .url("https://www.indianrail.gov.in/enquiry/captchaDraw.png?${timestamp}=")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0")
            .header("Accept", "image/avif,image/webp,*/*")
            .header("Referer", "https://www.indianrail.gov.in/enquiry/PNR/PnrEnquiry.html?locale=en")
            .build()

        val response = httpClient.newCall(captchaRequest).execute()
        if (response.isSuccessful) {
            val bytes = response.body?.bytes()
            if (bytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    return Result.Success(bitmap)
                }
            }
        }
        return Result.Error(Exception("Failed to fetch captcha: ${response.code}"))
    }

    suspend fun queryPnrStatus(pnr: String, captchaAnswer: String): Result<PnrResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.indianrail.gov.in/enquiry/CommonCaptcha" +
                    "?inputCaptcha=${captchaAnswer}" +
                    "&inputPnrNo=${pnr}" +
                    "&inputPage=PNR" +
                    "&language=en"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0")
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://www.indianrail.gov.in/enquiry/PNR/PnrEnquiry.html?locale=en")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    // Detect session expiry before any other check
                    if (body.contains("errorMessage") && body.contains("Session out", ignoreCase = true)) {
                        return@withContext Result.SessionError
                    }
                    // Detect captcha mismatch response before parsing as PnrResponse
                    if (body.contains("errorMessage") && body.contains("Captcha not matched", ignoreCase = true)) {
                        return@withContext Result.CaptchaError("Captcha not matched")
                    }
                    // Detect flushed / not-yet-generated PNR
                    if (body.contains("errorMessage") && body.contains("Flushed Pnr", ignoreCase = true)) {
                        val reason = runCatching {
                            gson.fromJson(body, Map::class.java)["errorMessage"] as? String
                        }.getOrNull() ?: "PNR not found or not yet generated."
                        return@withContext Result.InvalidPnr(reason)
                    }
                    val pnrResponse = gson.fromJson(body, PnrResponse::class.java)
                    if (pnrResponse.pnrNumber.isNotBlank()) {
                        return@withContext Result.Success(pnrResponse)
                    } else {
                        return@withContext Result.Error(Exception("Invalid PNR or wrong captcha. Please try again."))
                    }
                }
            }
            Result.Error(Exception("Server error: ${response.code}. Please try again."))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Re-fetches the PNR status using the **existing session** (no captcha needed).
     * The session cookie established during [queryPnrStatus] remains valid.
     */
    suspend fun refreshPnrStatus(pnr: String): Result<PnrResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.indianrail.gov.in/enquiry/CommonCaptcha" +
                    "?inputCaptcha=" +
                    "&inputPnrNo=${pnr}" +
                    "&inputPage=PNR" +
                    "&language=en"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0")
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://www.indianrail.gov.in/enquiry/PNR/PnrEnquiry.html?locale=en")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    // Detect session expiry
                    if (body.contains("errorMessage") && body.contains("Session out", ignoreCase = true)) {
                        return@withContext Result.SessionError
                    }
                    if (!body.contains("errorMessage")) {
                        val pnrResponse = gson.fromJson(body, PnrResponse::class.java)
                        if (pnrResponse.pnrNumber.isNotBlank()) {
                            return@withContext Result.Success(pnrResponse)
                        }
                    }
                }
            }
            Result.Error(Exception("Failed to refresh. Please try again."))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
