package com.vacart.repository

import com.vacart.model.StationStop
import com.vacart.model.StopStatus
import com.vacart.model.TrainRunningStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.vacart.provideHttpLoggingInterceptor

@Singleton
class TrainTrackingRepository @Inject constructor() {

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .addInterceptor(provideHttpLoggingInterceptor())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val baseUrl = "https://enquiry.indianrail.gov.in"
    private val trackingUrl = "$baseUrl/mntes/tr?opt=TrainRunning&subOpt=FindRunningInstance"

    /**
     * Fetches the train running status HTML page and parses it into [TrainRunningStatus].
     * @param trainNo  The train number (e.g. "12345")
     * @param date     Journey date in dd-MMM-yyyy format (e.g. "30-Aug-2026")
     */
    suspend fun fetchTrainRunningStatus(
        trainNo: String,
        date: String
    ): Result<TrainRunningStatus> = withContext(Dispatchers.IO) {
        try {
            cookieManager.cookieStore.removeAll()

            // Step 1: GET the base page to establish session cookies (JSESSIONID, TS..., SERVERID)
            val initRequest = Request.Builder()
                .url("$baseUrl/mntes/")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            val initResponse = httpClient.newCall(initRequest).execute()
            initResponse.close()

            // Step 2: Fetch the dynamic CSRF token generated for this session
            val timestamp = System.currentTimeMillis()
            val csrfRequest = Request.Builder()
                .url("$baseUrl/mntes/GetCSRFToken?t=$timestamp")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$baseUrl/mntes/")
                .build()

            val csrfResponse = httpClient.newCall(csrfRequest).execute()
            val csrfHtml = csrfResponse.body?.string() ?: ""
            csrfResponse.close()

            // Extract CSRF token name and value from response (supports single & double quotes)
            val hiddenToken = extractCsrfToken(csrfHtml)

            // Step 3: POST to get running status HTML
            val formBodyBuilder = FormBody.Builder()
                .add("lan", "en")
                .add("jDate", date)
                .add("trainNo", trainNo)

            if (hiddenToken != null) {
                formBodyBuilder.add(hiddenToken.first, hiddenToken.second)
            }

            val formBody = formBodyBuilder.build()

            val postRequest = Request.Builder()
                .url(trackingUrl)
                .post(formBody)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", baseUrl)
                .header("Referer", "$baseUrl/mntes/")
                .header("Upgrade-Insecure-Requests", "1")
                .build()

            val response = httpClient.newCall(postRequest).execute()
            val html = response.body?.string() ?: ""
            response.close()

            if (html.isBlank()) {
                return@withContext Result.Error(Exception("Empty response from server"))
            }

            val doc = Jsoup.parse(html)
            val status = parseTrainRunningStatus(doc)
                ?: return@withContext Result.Error(Exception("Could not find train data. Please check the train number and date."))

            Result.Success(status)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Extracts CSRF token name and value from GetCSRFToken HTML snippet. */
    private fun extractCsrfToken(html: String): Pair<String, String>? {
        if (html.isBlank()) return null
        return try {
            val nameMatch = Regex("""name=['"]([^'"]+)['"]""").find(html)
            val valMatch = Regex("""value=['"]([^'"]+)['"]""").find(html)
            if (nameMatch != null && valMatch != null) {
                Pair(nameMatch.groupValues[1], valMatch.groupValues[1])
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrainRunningStatus(doc: Document): TrainRunningStatus? {
        // 1. Train number and name (find h3 containing digits, excluding navbar titles like "Spot Your Train")
        val headerH3 = doc.select("h3").firstOrNull { h3 ->
            val txt = h3.text().trim()
            txt.any { it.isDigit() } && !txt.contains("spot", ignoreCase = true)
        } ?: doc.selectFirst("div.w3-panel.w3-round.w3-blue h3") ?: return null

        val headerText = headerH3.text().trim()
        if (headerText.isBlank()) return null

        val parts = headerText.split(Regex("\\s+"), limit = 2)
        val trainNumber = parts[0].trim()
        val trainName = if (parts.size > 1) parts[1].trim() else ""

        // 2. Current running status
        val currentStatus = doc.selectFirst("h6.text-primary b")?.text()?.trim() ?: ""

        // 3. Last updated timestamp
        val lastUpdatedH4 = doc.select("h4").firstOrNull { it.text().contains("Last Updates On", ignoreCase = true) }
        val lastUpdatedOn = lastUpdatedH4?.nextElementSibling()?.text()?.trim() ?: ""

        // 4. Start date (Find H4 corresponding to current/active position instance or matching requested date)
        val startDateH4s = doc.select("h4").filter { it.text().contains("Start Date", ignoreCase = true) }
        val activeH4 = startDateH4s.firstOrNull { h4 ->
            h4.text().contains("Current Position", ignoreCase = true)
        } ?: startDateH4s.firstOrNull()

        val startDate = activeH4?.text()?.substringAfter(":")?.trim() ?: ""

        // 5. Station stops (Collect stop rows strictly from active instance section in document order)
        val sectionStopRows = mutableListOf<org.jsoup.nodes.Element>()
        if (activeH4 != null) {
            val allElements = doc.allElements
            val startIndex = allElements.indexOf(activeH4)
            if (startIndex != -1) {
                for (i in (startIndex + 1) until allElements.size) {
                    val el = allElements[i]
                    if (el.tagName().equals("h4", ignoreCase = true) && el.text().contains("Start Date", ignoreCase = true)) {
                        break
                    }
                    if (el.hasClass("stopRow") || el.hasClass("nonStopRow")) {
                        sectionStopRows.add(el)
                    }
                }
            }
        }

        val rawStopRows = if (sectionStopRows.isNotEmpty()) sectionStopRows else doc.select("div.stopRow, div.nonStopRow, div.w3-card-2.stopRow, div.w3-card-2.nonStopRow")

        val stopRows = rawStopRows.filter { row ->
            val rStyle = row.attr("style").lowercase().replace(" ", "")
            val pStyle = row.parent()?.attr("style")?.lowercase()?.replace(" ", "") ?: ""
            !rStyle.contains("display:none") && !pStyle.contains("display:none") &&
                    rawStopRows.none { parent -> parent != row && row.parents().any { it == parent } }
        }

        val stops = mutableListOf<StationStop>()

        for (row in stopRows) {
            val isStop = row.hasClass("stopRow")

            var stationName = ""
            var stationCode = ""
            var distance = ""
            var platform = ""
            var scheduledArrival = ""
            var actualArrival = ""
            var arrivalDelay = ""
            var scheduledDeparture = ""
            var actualDeparture = ""
            var departureDelay = ""
            var isLiveLocation = false
            var updatedOn = ""
            var liveStatusText = ""
            var divyangjanInfo = ""
            val coachPositions = mutableListOf<com.vacart.model.CoachPositionInfo>()

            // 1. Check live train location indicator (green blinking dot gif)
            val isBlinkingDot = row.select("img[src*=\"green_dot_blink\"], img[src*=\"blink\"]").isNotEmpty()
            if (isBlinkingDot) {
                isLiveLocation = true
            }

            // 2. Updated On timestamp
            val updatedFont = row.select("font, span").firstOrNull { it.text().contains("Updated on", ignoreCase = true) }
            if (updatedFont != null) {
                val parentDiv = updatedFont.parents().firstOrNull { it.tagName().equals("div", ignoreCase = true) }
                val updatedB = parentDiv?.selectFirst("b") ?: updatedFont.selectFirst("b")
                if (updatedB != null) {
                    updatedOn = updatedB.text().trim()
                }
            }

            // 3. Live status text (e.g. Departed from ANKAI (ANK) on 30-Aug-2026 22:09)
            val greenFont = row.select("font[color=\"GREEN\"], font[color=\"green\"], font[color*=\"green\"]").firstOrNull()
            if (greenFont != null) {
                liveStatusText = greenFont.text().trim()
                if (liveStatusText.isNotBlank()) {
                    isLiveLocation = true
                }
            }

            // 4. Platform Number
            val pfSpan = row.select("span.w3-orange").firstOrNull { it.text().contains("PF", ignoreCase = true) }
            if (pfSpan != null) {
                platform = pfSpan.text().trim()
            }

            if (isStop) {
                // Station Name & Code inside Center container (flex:1)
                val center = row.selectFirst("div[style*=\"flex:1\"]")
                if (center != null) {
                    val bolds = center.select("b")
                    if (bolds.isNotEmpty()) {
                        stationName = bolds[0].ownText().trim().ifBlank { bolds[0].text().trim() }
                    }
                    if (bolds.size > 1) {
                        val codeElem = bolds[1].clone()
                        codeElem.select("span").remove()
                        val rawCode = codeElem.text().trim()
                        stationCode = if (rawCode.isNotBlank()) rawCode.split(Regex("\\s+"))[0] else ""
                    }
                    for (b in bolds) {
                        val parentText = b.parent()?.text() ?: ""
                        val bText = b.text().trim()
                        if (parentText.contains("KMs", ignoreCase = true) && bText.all { it.isDigit() }) {
                            distance = "$bText KMs"
                            break
                        }
                    }
                }

                // Scheduled & Actual Arrival & Delay from left container
                val left = row.selectFirst("div[style*=\"float:left\"][style*=\"100px\"], div[style*=\"float:left\"]")
                if (left != null) {
                    val fonts = left.select("font")
                    if (fonts.isNotEmpty()) {
                        val schB = fonts[0].selectFirst("b")
                        scheduledArrival = schB?.text()?.trim() ?: fonts[0].text().trim()
                    }
                    if (fonts.size > 1) {
                        val actB = fonts[1].selectFirst("b")
                        actualArrival = actB?.text()?.trim() ?: ""
                        val delaySpan = fonts[1].selectFirst("span.w3-round")
                        if (delaySpan != null) {
                            arrivalDelay = delaySpan.text().trim()
                        }
                    }
                }

                // Scheduled & Actual Departure & Delay from right container
                val right = row.selectFirst("div[style*=\"float:right\"][style*=\"text-align:right\"], div[style*=\"float:right\"]")
                if (right != null) {
                    val fonts = right.select("font")
                    if (fonts.isNotEmpty()) {
                        val schB = fonts[0].selectFirst("b")
                        scheduledDeparture = schB?.text()?.trim() ?: fonts[0].text().trim()
                    }
                    if (fonts.size > 1) {
                        val actB = fonts[1].selectFirst("b")
                        actualDeparture = actB?.text()?.trim() ?: ""
                        val delaySpan = fonts[1].selectFirst("span.w3-round")
                        if (delaySpan != null) {
                            departureDelay = delaySpan.text().trim()
                        }
                    }
                }
            } else {
                // Non-stopping station (nonStopRow): "STATION NAME - CODE" in first <b> tag
                val center = row.selectFirst("div[style*=\"flex:1\"]")
                if (center != null) {
                    val bolds = center.select("b")
                    if (bolds.isNotEmpty()) {
                        val titleText = bolds[0].text().trim()
                        if (titleText.contains("-")) {
                            val nameCodeParts = titleText.split("-")
                            stationName = nameCodeParts[0].trim()
                            stationCode = nameCodeParts[1].trim()
                        } else {
                            stationName = titleText
                            stationCode = titleText
                        }
                    }
                    for (b in bolds) {
                        val parentText = b.parent()?.text() ?: ""
                        val bText = b.text().trim()
                        if (parentText.contains("KMs", ignoreCase = true) && bText.all { it.isDigit() }) {
                            distance = "$bText KMs"
                            break
                        }
                    }
                }
            }

            // 5. Coach position modal parsing
            val modalBtn = row.selectFirst("button[data-bs-target]")
            val modalId = modalBtn?.attr("data-bs-target")?.removePrefix("#")
            val modal = if (!modalId.isNullOrBlank()) doc.selectFirst("div.modal#$modalId") else row.selectFirst("div.modal")
            if (modal != null) {
                val coachDivs = modal.select("div[style*=\"45px\"][style*=\"60px\"]")
                for (cd in coachDivs) {
                    val innerDivs = cd.select("div")
                    if (innerDivs.size >= 3) {
                        val cType = innerDivs[0].text().trim()
                        val cName = innerDivs[1].selectFirst("b")?.text()?.trim() ?: innerDivs[1].text().trim()
                        val cPos = innerDivs[2].text().trim()
                        coachPositions.add(
                            com.vacart.model.CoachPositionInfo(
                                coachType = cType,
                                coachName = cName,
                                positionIndex = cPos
                            )
                        )
                    }
                }
                val divFont = modal.selectFirst("font[color=\"red\"]")
                if (divFont != null) {
                    divyangjanInfo = divFont.text().trim()
                }
            }

            val finalStationCode = if (stationCode.isNotBlank()) stationCode else stationName
            val finalStationName = if (stationName.isNotBlank()) stationName else stationCode

            if (finalStationCode.isBlank() && finalStationName.isBlank()) {
                continue
            }

            // Deduplication: break if station list begins repeating once start to end finishes
            if (stops.isNotEmpty()) {
                val firstStop = stops.first()
                val isDuplicateWithFirst = (finalStationCode.equals(firstStop.stationCode, ignoreCase = true) ||
                        finalStationName.equals(firstStop.stationName, ignoreCase = true)) &&
                        (scheduledDeparture.isBlank() || scheduledDeparture == firstStop.scheduledDeparture)
                val isDuplicateWithAny = stops.any {
                    it.stationCode.equals(finalStationCode, ignoreCase = true) &&
                    it.scheduledArrival == scheduledArrival &&
                    it.scheduledDeparture == scheduledDeparture &&
                    it.distance == distance
                }
                if (isDuplicateWithFirst || isDuplicateWithAny) {
                    break
                }
            }

            val rowText = row.text().lowercase()
            val rowClass = row.className().lowercase()
            val stopStatus = when {
                isLiveLocation -> {
                    if (liveStatusText.contains("Departed", ignoreCase = true) || rowClass.contains("departed") || rowText.contains("departed")) {
                        StopStatus.DEPARTED
                    } else {
                        StopStatus.AT_STATION
                    }
                }
                rowClass.contains("at_station") || rowClass.contains("arrived") || rowClass.contains("current") ||
                        rowText.contains("at station") || rowText.contains("standing at") -> StopStatus.AT_STATION
                rowClass.contains("departed") || rowText.contains("departed") -> StopStatus.DEPARTED
                else -> if (actualDeparture.isNotBlank()) StopStatus.DEPARTED
                        else if (actualArrival.isNotBlank()) StopStatus.AT_STATION
                        else if (!isStop) StopStatus.SKIPPED
                        else StopStatus.UPCOMING
            }

            stops.add(
                StationStop(
                    stationCode = finalStationCode,
                    stationName = finalStationName,
                    scheduledArrival = scheduledArrival,
                    actualArrival = actualArrival,
                    arrivalDelay = arrivalDelay,
                    scheduledDeparture = scheduledDeparture,
                    actualDeparture = actualDeparture,
                    departureDelay = departureDelay,
                    platform = platform,
                    distance = distance,
                    isStop = isStop,
                    delayMinutes = null,
                    status = stopStatus,
                    isLiveLocation = isLiveLocation,
                    updatedOn = updatedOn,
                    liveStatusText = liveStatusText,
                    coachPositions = coachPositions,
                    divyangjanInfo = divyangjanInfo
                )
            )
        }

        return TrainRunningStatus(
            trainNumber = trainNumber,
            trainName = trainName,
            currentStatus = currentStatus,
            lastUpdatedOn = lastUpdatedOn,
            startDate = startDate,
            stops = stops
        )
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0"
    }
}
