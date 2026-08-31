package com.vacart.repository

import android.util.Log
import com.vacart.model.CoachPositionInfo
import com.vacart.model.StationStop
import com.vacart.model.StopStatus
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
     * Returns a list of (startDate, jStation) pairs that can be fed into
     * [fetchTrainRunningStatus].
     */
    suspend fun fetchTrainInstances(
        trainNo: String
    ): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
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
                // Log top-level keys (safe, small) + write full JSON to file
                try {
                    val topObj = JSONObject(decryptedJson)
                    val keys = topObj.keys().asSequence().toList()
                    Log.d("NtesApi", "Top-level keys [${queryString.substringAfter("subService=")}]: $keys")
                    // Log first station entry if present
                    val arr = topObj.optJSONArray("stationList")
                        ?: topObj.optJSONArray("TrainInfo")
                        ?: topObj.optJSONArray("trainInfo")
                        ?: topObj.optJSONArray("StationList")
                    if (arr != null && arr.length() > 0) {
                        val firstStn = arr.getJSONObject(0)
                        Log.d("NtesApi", "First station keys: ${firstStn.keys().asSequence().toList()}")
                        Log.d("NtesApi", "First station data: ${firstStn}")
                    } else {
                        // No station array found - log full response (truncated to 3000 chars)
                        Log.d("NtesApi", "Full response (3000c): ${decryptedJson.take(3000)}")
                    }
                } catch (e: Exception) {
                    Log.e("NtesApi", "Debug log failed", e)
                }
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
     * Parses the `ShowFullRunJson` decrypted JSON response into [TrainRunningStatus].
     *
     * Expected top-level keys (representative — actual keys confirmed from NTES APK sources):
     *  - `trainName` / `trainNo`
     *  - `currentStatus` or `position`
     *  - `updateTime` / `updatedOn`
     *  - `startDate`
     *  - `stationList` — array of station objects
     */
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

    private fun parseShowFullRunJson(json: JSONObject): TrainRunningStatus {
        val trnRunCls = json.optJSONObject("trnRunCls")
        val trainObj  = json.optJSONObject("train")

        val trainNumber = json.optString("TN").ifBlank { null }
            ?: trnRunCls?.optString("trainNumber")?.ifBlank { null }
            ?: trainObj?.optString("TrainNumber")?.ifBlank { null }
            ?: json.optString("trainNo").ifBlank { json.optString("trainNumber") }

        val trainName = json.optString("TNM").ifBlank { null }
            ?: trnRunCls?.optString("TrainName")?.ifBlank { null }
            ?: trainObj?.optString("TrainName")?.ifBlank { null }
            ?: json.optString("trainName")

        val lastUpdatedOn = json.optString("LUPDFULL").ifBlank { null }
            ?: json.optString("LUPDT")?.ifBlank { null }
            ?: json.optString("LASTUPD")?.ifBlank { null }
            ?: trnRunCls?.optString("LastUpdateFull")?.ifBlank { null }
            ?: trnRunCls?.optString("LastUpdate")?.ifBlank { null }
            ?: json.optString("updateTime").ifBlank { json.optString("updatedOn") }

        val startDate = json.optString("STD").ifBlank { null }
            ?: trnRunCls?.optString("StartDate")?.ifBlank { null }
            ?: trainObj?.optString("StartDate")?.ifBlank { null }
            ?: json.optString("startDate").ifBlank { json.optString("journeyDate") }

        val sourceStation = json.optString("SRC").ifBlank { null }
            ?: trnRunCls?.optString("Source")?.ifBlank { null }
            ?: trainObj?.optString("Source")?.ifBlank { null }
            ?: json.optString("srcStn").ifBlank { json.optString("source") }

        val sourceStationName = json.optString("SRCN").ifBlank { null }
            ?: trnRunCls?.optString("SourceName")?.ifBlank { null }
            ?: trainObj?.optString("SourceName")?.ifBlank { null }
            ?: json.optString("srcStnName").ifBlank { json.optString("sourceName") }

        val destStation = json.optString("DSTN").ifBlank { null }
            ?: trnRunCls?.optString("Destination")?.ifBlank { null }
            ?: trainObj?.optString("Destination")?.ifBlank { null }
            ?: json.optString("destStn").ifBlank { json.optString("destination") }

        val destStationName = json.optString("DSTNN").ifBlank { null }
            ?: trnRunCls?.optString("DestinationName")?.ifBlank { null }
            ?: trainObj?.optString("DestinationName")?.ifBlank { null }
            ?: json.optString("destStnName").ifBlank { json.optString("destinationName") }

        val totalDistance = json.optString("TTLDIST").ifBlank { null }
            ?: trnRunCls?.optString("TotalTrainDistance")?.ifBlank { null }
            ?: json.optString("totalDist").ifBlank { json.optString("totalDistance") }

        val trainType = trainObj?.optString("TypeDesc")?.ifBlank { null }
            ?: trainObj?.optString("Type")?.ifBlank { null }
            ?: json.optString("trainType")

        val classes = trnRunCls?.optString("jsArrivalCoachClass")?.ifBlank { null }
            ?: trnRunCls?.optString("jsDepartureCoachClass")?.ifBlank { null }
            ?: json.optString("classes")

        val lastEvent = json.optString("LEVNT").ifBlank { null }
            ?: trnRunCls?.optString("LastEvent")?.ifBlank { "" } ?: ""
        val lastStationName = json.optString("LSTNN").ifBlank { null }
            ?: trnRunCls?.optString("LastStationName")?.ifBlank { "" } ?: ""
        val currentStatus = if (lastEvent.isNotBlank()) {
            if (lastStationName.isNotBlank() && !lastEvent.contains(lastStationName, ignoreCase = true)) {
                "$lastEvent ($lastStationName)"
            } else lastEvent
        } else {
            json.optString("currentStatus").ifBlank {
                json.optString("position").ifBlank { json.optString("trainStatus") }
            }
        }

        val ldelStr = json.optString("LDEL")
        val currentDelayMins = parseDelayToMins(ldelStr)
            ?: trnRunCls?.optString("DelayArrMin")?.toIntOrNull()
            ?: trnRunCls?.optString("DelayDepMin")?.toIntOrNull()
            ?: json.optInt("currentDelay", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            ?: json.optInt("delayMins", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

        val stationsArray: JSONArray = json.optJSONArray("STNS")
            ?: json.optJSONArray("fullRunningStns")
            ?: json.optJSONArray("stationList")
            ?: json.optJSONArray("stations")
            ?: JSONArray()

        val stops = mutableListOf<StationStop>()
        for (i in 0 until stationsArray.length()) {
            val stnObj = stationsArray.getJSONObject(i)
            // Main stopping station
            stops.add(parseStationStop(stnObj, isStopOverride = true))

            // Non-stopping passing stations under WTTSTNS
            val wttArray = stnObj.optJSONArray("WTTSTNS")
            if (wttArray != null) {
                for (j in 0 until wttArray.length()) {
                    val wttObj = wttArray.getJSONObject(j)
                    stops.add(parseStationStop(wttObj, isStopOverride = false))
                }
            }
        }

        return TrainRunningStatus(
            trainNumber       = trainNumber,
            trainName         = trainName,
            currentStatus     = currentStatus,
            lastUpdatedOn     = lastUpdatedOn,
            startDate         = startDate,
            stops             = stops,
            sourceStation     = sourceStation,
            sourceStationName = sourceStationName,
            destStation       = destStation,
            destStationName   = destStationName,
            totalDistance     = totalDistance,
            trainType         = trainType,
            classes           = classes,
            currentDelayMins  = currentDelayMins
        )
    }

    private fun parseStationStop(stn: JSONObject, isStopOverride: Boolean? = null): StationStop {
        val stationCode = stn.optString("SC").ifBlank {
            stn.optString("station").ifBlank {
                stn.optString("stnCode").ifBlank { stn.optString("stationCode") }
            }
        }
        val stationName = stn.optString("SN").ifBlank {
            stn.optString("stationName").ifBlank {
                stn.optString("stnName")
            }
        }

        val schArrRaw = stn.optString("STA").ifBlank {
            stn.optString("STA_HHMM").ifBlank {
                stn.optString("schArrTime").ifBlank { stn.optString("scheduledArrival") }
            }
        }
        val schArr = if (schArrRaw.equals("Source", ignoreCase = true)) "" else schArrRaw

        val actArrRaw = stn.optString("ETA").ifBlank {
            stn.optString("ETA_HHMM").ifBlank {
                stn.optString("actArrTime").ifBlank { stn.optString("actualArrival") }
            }
        }
        val actArr = if (actArrRaw.equals("Source", ignoreCase = true)) "" else actArrRaw

        val arrDelay = stn.optString("DARR").ifBlank {
            stn.optString("delayArr").ifBlank { stn.optString("arrDelay") }
        }

        val schDep = stn.optString("STD").ifBlank {
            stn.optString("STD_HHMM").ifBlank {
                stn.optString("schDepTime").ifBlank { stn.optString("scheduledDeparture") }
            }
        }
        val actDep = stn.optString("ETD").ifBlank {
            stn.optString("ETD_HHMM").ifBlank {
                stn.optString("actDepTime").ifBlank { stn.optString("actualDeparture") }
            }
        }
        val depDelay = stn.optString("DDEP").ifBlank {
            stn.optString("delayDep").ifBlank { stn.optString("depDelay") }
        }

        val platformRaw = stn.optString("PF").ifBlank {
            stn.optString("pfNumber").ifBlank {
                stn.optString("pfNo").ifBlank { stn.optString("platform") }
            }
        }
        val platform = if (platformRaw.isNotBlank() && !platformRaw.startsWith("PF", ignoreCase = true)) {
            "PF $platformRaw"
        } else platformRaw

        val distOpt = stn.opt("DIST") ?: stn.opt("distanceFromSource") ?: stn.opt("distance")
        val distVal = distOpt?.toString() ?: ""
        val distance = if (distVal.isNotBlank() && !distVal.contains("km", ignoreCase = true)) {
            "$distVal KMs"
        } else distVal

        val isStop = isStopOverride ?: run {
            val stopTypeRaw = stn.optString("haltType").ifBlank { stn.optString("stopType") }
            stopTypeRaw.equals("S", ignoreCase = true) || stopTypeRaw.isBlank() || stn.optBoolean("isHalt", true)
        }

        val delayMins = parseDelayToMins(arrDelay)
            ?: parseDelayToMins(depDelay)
            ?: stn.optInt("delayMins", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

        val haltMins = stn.optInt("haltTime", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            ?: stn.optInt("halt", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

        val dayCount = stn.optInt("DF", 0).takeIf { it >= 0 }
            ?: stn.optInt("dayCount", 0)

        val isa = if (stn.has("ISA")) stn.optBoolean("ISA") else null
        val isd = if (stn.has("ISD")) stn.optBoolean("ISD") else null

        val liveStatusText = stn.optString("trainActStatus").ifBlank {
            stn.optString("liveStatus")
        }
        val updatedOn = stn.optString("updatedOn").ifBlank { stn.optString("updateTime") }

        val isLive = (isa == true && isd == false) ||
                stn.optBoolean("isCurrent", false) ||
                stn.optBoolean("isLive", false)

        val coachPositions = mutableListOf<CoachPositionInfo>()
        val arrivalCoachPos = stn.optString("arrivalCoachPosition")
        val arrivalCoachClass = stn.optString("arrivalCoachClass")
        if (arrivalCoachPos.isNotBlank()) {
            val posTokens = arrivalCoachPos.split("-").map { it.trim() }
            val classTokens = arrivalCoachClass.split("-").map { it.trim() }
            posTokens.forEachIndexed { index, name ->
                val cls = classTokens.getOrNull(index) ?: ""
                coachPositions.add(
                    CoachPositionInfo(
                        coachType = cls,
                        coachName = name,
                        positionIndex = (index + 1).toString()
                    )
                )
            }
        }

        val divyangjanInfo = stn.optString("arrPWDCoachPosition").let { pwd ->
            if (pwd.isNotBlank()) "Divyangjan Coach at #$pwd" else stn.optString("divyangjanInfo")
        }

        val stopStatus = when {
            isd == true -> StopStatus.DEPARTED
            isa == true -> StopStatus.AT_STATION
            else -> resolveStopStatus(
                isLive = isLive,
                liveStatusText = liveStatusText,
                actArr = actArr,
                actDep = actDep,
                isStop = isStop,
                statusStr = liveStatusText
            )
        }

        return StationStop(
            stationCode = stationCode,
            stationName = stationName,
            scheduledArrival = schArr,
            actualArrival = actArr,
            arrivalDelay = arrDelay,
            scheduledDeparture = schDep,
            actualDeparture = actDep,
            departureDelay = depDelay,
            platform = platform,
            distance = distance,
            isStop = isStop,
            delayMinutes = delayMins,
            haltMinutes = haltMins,
            dayCount = dayCount,
            status = stopStatus,
            isLiveLocation = isLive,
            updatedOn = updatedOn,
            liveStatusText = liveStatusText,
            coachPositions = coachPositions,
            divyangjanInfo = divyangjanInfo
        )
    }

    private fun resolveStopStatus(
        isLive: Boolean,
        liveStatusText: String,
        actArr: String,
        actDep: String,
        isStop: Boolean,
        statusStr: String
    ): StopStatus = when {
        isLive && liveStatusText.contains("Departed", ignoreCase = true) -> StopStatus.DEPARTED
        isLive -> StopStatus.AT_STATION
        statusStr.contains("Departed", ignoreCase = true)  -> StopStatus.DEPARTED
        statusStr.contains("Arrived", ignoreCase = true) ||
            statusStr.contains("At Station", ignoreCase = true) -> StopStatus.AT_STATION
        actDep.isNotBlank() -> StopStatus.DEPARTED
        actArr.isNotBlank() -> StopStatus.AT_STATION
        !isStop              -> StopStatus.SKIPPED
        else                 -> StopStatus.UPCOMING
    }

    private fun parseTrainInstances(json: JSONObject): List<Pair<String, String>> {
        val instances = mutableListOf<Pair<String, String>>()
        val arr = json.optJSONArray("trainInstances")
            ?: json.optJSONArray("instances")
            ?: return instances
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val date = obj.optString("startDate").ifBlank { obj.optString("jDate") }
            val stn  = obj.optString("jStation").ifBlank { obj.optString("station") }
            if (date.isNotBlank()) instances.add(Pair(date, stn))
        }
        return instances
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildQuery(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "$k=$v" }

    companion object {
        private const val USER_AGENT =
            "Dalvik/2.1.0 (Linux; U; Android 14; Build/UP1A.231005.007)"
    }
}
