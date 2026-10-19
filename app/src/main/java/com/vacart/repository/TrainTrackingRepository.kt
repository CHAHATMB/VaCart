package com.vacart.repository

import android.util.Log
import com.vacart.model.CoachPositionInfo
import com.vacart.model.StationStop
import com.vacart.model.StopStatus
import com.vacart.model.TrainInstance
import com.vacart.model.TrainRunningStatus
import com.vacart.util.NtesCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.vacart.BuildConfig
import com.vacart.provideHttpLoggingInterceptor

/**
 * Fetches live train running status from the official NTES mobile JSON API.
 *
 * Replaces the previous HTML-scraping implementation that relied on Jsoup +
 * enquiry.indianrail.gov.in/mntes/tr session cookies.
 *
 * API base: https://enquiry.indianrail.gov.in/crisns/AppServAnd
 * Encryption: AES-128-CBC (key/iv from NTES BuildConfig), payload signed with MD5.
 * See ntes_api_documentation.md for full specification.
 */
@Singleton
class TrainTrackingRepository @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(provideHttpLoggingInterceptor())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://enquiry.indianrail.gov.in/crisns/AppServAnd"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches full live train running status using the `ShowFullRunJson` endpoint.
     *
     * @param trainNo      Train number (e.g. "12002")
     * @param date         Journey start date in dd-MMM-yyyy format (e.g. "30-Aug-2026")
     * @param stationCode  Boarding station code (e.g. "NDLS"). Defaults to empty string
     *                     which makes the server return status for the entire route.
     */
    suspend fun fetchTrainRunningStatus(
        trainNo: String,
        date: String,
        stationCode: String = ""
    ): Result<TrainRunningStatus> = withContext(Dispatchers.IO) {
        val query = buildQuery(
            "service" to "TrainRunningMob",
            "subService" to "ShowFullRunJson",
            "trainNo" to trainNo,
            "jStation" to stationCode,
            "startDate" to date
        )
        ntesCryptoRequest(query) { parseShowFullRunJson(it) }
    }

    /**
     * Fetches active running instances for a train number via `GetTrainInstance`.
     * Returns a list of [TrainInstance] objects that can be displayed to the user
     * for selection, then fed into [fetchTrainRunningStatus].
     */
    suspend fun fetchTrainInstances(
        trainNo: String
    ): Result<List<TrainInstance>> = withContext(Dispatchers.IO) {
        val query = buildQuery(
            "service" to "TrainRunningMob",
            "subService" to "GetTrainInstance",
            "trainNo" to trainNo
        )
        ntesCryptoRequest(query) { parseTrainInstances(it) }
    }

    // ── Core NTES request handler ─────────────────────────────────────────────

    private fun <T> ntesCryptoRequest(
        queryString: String,
        parse: (JSONObject) -> T
    ): Result<T> {
        return try {
            // 1. Build headers (one-time meta security header)
            val (metaKey, metaVal) = NtesCrypto.generateMetaHeader()

            // 2. Encrypt query string → {"jsonIn": "MD5HASH#HEXENCRYPTED"}
            val encryptedPayload = NtesCrypto.encryptPayload(queryString)
            val requestJson = JSONObject().put("jsonIn", encryptedPayload).toString()
            val body = requestJson.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(baseUrl)
                .post(body)
                .header("Content-Type", "application/json")
                .header("charset", "utf-8")
                .header(metaKey, metaVal)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            response.close()

            if (responseStr.isBlank()) {
                return Result.Error(Exception("Empty response from NTES server"))
            }

            // 3. Decrypt {"jsonIn": "<HEX_ENCRYPTED>"} → plain JSON string
            val responseJson = JSONObject(responseStr)
            val hexEncrypted = responseJson.optString("jsonIn")
            if (hexEncrypted.isBlank()) {
                return Result.Error(Exception("Missing jsonIn field in NTES response"))
            }

            val decryptedJson = NtesCrypto.decryptResponse(hexEncrypted)
            if (BuildConfig.DEBUG) {
                val endpoint = queryString.substringAfter("subService=").substringBefore("&")
                Log.d("NtesApi", "Response [$endpoint]: ${decryptedJson.take(3000)}")
            }

            val parsedData = parse(JSONObject(decryptedJson))
            Result.Success(parsedData)

        } catch (e: Exception) {
            Log.e("NtesApi", "Request failed for [$queryString]", e)
            Result.Error(e)
        }
    }

    // ── JSON Parsers ──────────────────────────────────────────────────────────

    /**
     * Parses the `ShowFullRunJson` decrypted response into [TrainRunningStatus].
     *
     * Field names match the documented NTES API response exactly:
     * TN, TNM, LUPDFULL, LUPDT, STD, SRC, SRCN, DSTN, DSTNN, TTLDIST,
     * LDEL, LEVNT, LSTNN, CPOS, STNS.
     */
    private fun parseShowFullRunJson(json: JSONObject): TrainRunningStatus {
        val lastEvent = json.optString("LEVNT")
        val lastStationName = json.optString("LSTNN")
        val currentStatus = when {
            lastEvent.isNotBlank() && lastStationName.isNotBlank() -> "$lastEvent ($lastStationName)"
            lastEvent.isNotBlank() -> lastEvent
            else -> json.optString("CPOS")
        }

        val stationsArray = json.optJSONArray("STNS") ?: JSONArray()
        val stops = buildList {
            for (i in 0 until stationsArray.length()) {
                add(parseStationStop(stationsArray.getJSONObject(i)))
            }
        }

        return TrainRunningStatus(
            trainNumber       = json.optString("TN"),
            trainName         = json.optString("TNM"),
            currentStatus     = currentStatus,
            lastUpdatedOn     = json.optString("LUPDFULL").ifBlank { json.optString("LUPDT") },
            startDate         = json.optString("STD"),
            stops             = stops,
            sourceStation     = json.optString("SRC"),
            sourceStationName = json.optString("SRCN"),
            destStation       = json.optString("DSTN"),
            destStationName   = json.optString("DSTNN"),
            totalDistance     = json.optString("TTLDIST"),
            currentDelayMins  = parseDelayToMins(json.optString("LDEL"))
        )
    }

    /**
     * Parses a single station object from the `STNS` array.
     *
     * Field names per documented API: SC, SN, SHN, STA, ETA, DARR, STD, ETD,
     * DDEP, PF, DIST, DF, ISA, ISD, arrivalCoachPosition, arrivalCoachClass,
     * arrPWDCoachPosition.
     */
    private fun parseStationStop(stn: JSONObject): StationStop {
        val isa = if (stn.has("ISA")) stn.optBoolean("ISA") else null
        val isd = if (stn.has("ISD")) stn.optBoolean("ISD") else null

        val schArr = stn.optString("STA").let { if (it.equals("Source", ignoreCase = true)) "" else it }
        val actArr = stn.optString("ETA").let { if (it.equals("Source", ignoreCase = true)) "" else it }
        val arrDelay = stn.optString("DARR")
        val schDep   = stn.optString("STD")
        val actDep   = stn.optString("ETD")
        val depDelay = stn.optString("DDEP")

        val platformRaw = stn.optString("PF")
        val platform = if (platformRaw.isNotBlank() && !platformRaw.startsWith("PF", ignoreCase = true)) {
            "PF $platformRaw"
        } else platformRaw

        val distVal = stn.optString("DIST")
        val distance = if (distVal.isNotBlank() && distVal != "0" && !distVal.contains("km", ignoreCase = true)) {
            "$distVal KMs"
        } else distVal

        val isLive = isa == true && isd == false

        val stopStatus = when {
            isd == true          -> StopStatus.DEPARTED
            isa == true          -> StopStatus.AT_STATION
            actDep.isNotBlank()  -> StopStatus.DEPARTED
            actArr.isNotBlank()  -> StopStatus.AT_STATION
            else                 -> StopStatus.UPCOMING
        }

        return StationStop(
            stationCode       = stn.optString("SC"),
            stationName       = stn.optString("SN"),
            scheduledArrival  = schArr,
            actualArrival     = actArr,
            arrivalDelay      = arrDelay,
            scheduledDeparture = schDep,
            actualDeparture   = actDep,
            departureDelay    = depDelay,
            platform          = platform,
            distance          = distance,
            isStop            = true,
            delayMinutes      = parseDelayToMins(arrDelay) ?: parseDelayToMins(depDelay),
            haltMinutes       = null,
            dayCount          = stn.optInt("DF", 0),
            status            = stopStatus,
            isLiveLocation    = isLive,
            updatedOn         = "",
            liveStatusText    = "",
            coachPositions    = buildCoachPositions(
                positionStr = stn.optString("arrivalCoachPosition"),
                classStr    = stn.optString("arrivalCoachClass")
            ),
            divyangjanInfo    = stn.optString("arrPWDCoachPosition").let { pwd ->
                if (pwd.isNotBlank()) "Divyangjan Coach at #$pwd" else ""
            }
        )
    }

    private fun buildCoachPositions(positionStr: String, classStr: String): List<CoachPositionInfo> {
        if (positionStr.isBlank()) return emptyList()
        val positions = positionStr.split("-").map { it.trim() }
        val classes   = classStr.split("-").map { it.trim() }
        return positions.mapIndexed { index, name ->
            CoachPositionInfo(
                coachType     = classes.getOrElse(index) { "" },
                coachName     = name,
                positionIndex = (index + 1).toString()
            )
        }
    }

    private fun parseDelayToMins(delayStr: String?): Int? {
        if (delayStr.isNullOrBlank()) return null
        val trimmed = delayStr.trim()
        val parts = trimmed.split(":")
        return if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            h * 60 + m
        } else {
            trimmed.toIntOrNull()
        }
    }

    private fun parseTrainInstances(json: JSONObject): List<TrainInstance> {
        val trainNo  = json.optString("TrainNo")
        val trainName = json.optString("TrainName")
        val srcCode  = json.optString("Src")
        val srcName  = json.optString("SrcName")
        val dstnCode = json.optString("Dstn")
        val dstnName = json.optString("DstnName")

        val arr: JSONArray = json.optJSONArray("vInstanceList") ?: return emptyList()

        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val startDate = obj.optString("startDate")
                if (startDate.isBlank()) continue
                add(
                    TrainInstance(
                        startDate     = startDate,
                        trainPosition = obj.optString("trainPosition"),
                        trainStatus   = obj.optInt("trainStatus", 0),
                        trainNo       = trainNo,
                        trainName     = trainName,
                        srcCode       = srcCode,
                        srcName       = srcName,
                        dstnCode      = dstnCode,
                        dstnName      = dstnName,
                        excpMsg       = obj.optString("excpMsg")
                    )
                )
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildQuery(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "$k=$v" }

    companion object {
        private const val USER_AGENT =
            "Dalvik/2.1.0 (Linux; U; Android 14; Build/UP1A.231005.007)"
    }
}
