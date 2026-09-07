package com.vacart.presentation.tracking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vacart.model.StationStop
import com.vacart.model.StopStatus
import com.vacart.model.TrainInfo
import com.vacart.model.TrainRunningStatus
import com.vacart.presentation.common.ClickableInputField
import com.vacart.presentation.common.TrainSearchAutoCompleteField
import com.vacart.presentation.common.TrainSearchBottomSheet
import com.vacart.util.FeatureFlags
import com.vacart.util.getFormattedDateForNtes
import kotlinx.coroutines.launch

// ─── Shared colours ────────────────────────────────────────────────────────────
private val GreenLive   = Color(0xFF2E7D32)
private val GreenLight  = Color(0xFFE8F5E9)
private val GreenDot    = Color(0xFF4CAF50)
private val AmberDelay  = Color(0xFFFF8F00)
private val AmberLight  = Color(0xFFFFF8E1)
private val RedDelay    = Color(0xFFB71C1C)

// ─── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainTrackingScreen(viewModel: TrainTrackingViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Train Tracker",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                navigationIcon = {
                    if (state.step != TrackingStep.INPUT) {
                        IconButton(onClick = { viewModel.onEvent(TrainTrackingEvent.Reset) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (state.step == TrackingStep.RESULT) {
                        IconButton(
                            onClick = { viewModel.onEvent(TrainTrackingEvent.Refresh) },
                            enabled = !state.isRefreshing
                        ) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) { step ->
            when (step) {
                TrackingStep.INPUT -> TrainInputStep(
                    state = state,
                    onTrainNoChange = { viewModel.onEvent(TrainTrackingEvent.UpdateTrainNo(it)) },
                    onSuggestionSelected = { viewModel.onEvent(TrainTrackingEvent.SelectTrainInfo(it)) },
                    onDateOptionSelected = { label, dateStr ->
                        viewModel.onEvent(TrainTrackingEvent.UpdateDateOption(label, dateStr))
                    },
                    onSearch = { viewModel.onEvent(TrainTrackingEvent.Search) }
                )
                TrackingStep.RESULT -> {
                    val runningStatus = state.runningStatus ?: return@AnimatedContent
                    TrainResultStep(
                        status = runningStatus,
                        errorMessage = state.errorMessage,
                        onTrackAnother = { viewModel.onEvent(TrainTrackingEvent.Reset) }
                    )
                }
            }
        }
    }
}

// ─── Input step ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainInputStep(
    state: TrainTrackingState,
    onTrainNoChange: (String) -> Unit,
    onSuggestionSelected: (TrainInfo) -> Unit,
    onDateOptionSelected: (String, String) -> Unit,
    onSearch: () -> Unit
) {
    var showTrainBottomSheet by remember { mutableStateOf(false) }
    var showDateBottomSheet by remember { mutableStateOf(false) }
    val dateList = arrayOf("2 days ago", "Yesterday", "Today", "Tomorrow")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Track Live Train Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter train number or name and select journey date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Train Input Field (Bottom Sheet vs Dropdown Feature Flag)
                if (FeatureFlags.isBottomSheetSearchEnabled) {
                    ClickableInputField(
                        value = state.selectedTrain,
                        label = "Train Number / Name",
                        placeholder = "Tap to search train number or name",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (state.selectedTrain.isNotEmpty()) {
                                IconButton(onClick = { onTrainNoChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear train",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search train",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        isError = state.errorMessage != null && state.selectedTrain.isEmpty(),
                        onClick = {
                            showTrainBottomSheet = true
                        }
                    )
                } else {
                    TrainSearchAutoCompleteField(
                        searchQuery = state.selectedTrain,
                        filteredTrains = state.filteredTrains,
                        showError = state.errorMessage != null && state.selectedTrain.isEmpty(),
                        onValueChange = onTrainNoChange,
                        onSuggestionSelected = onSuggestionSelected,
                        onClearInput = {
                            onTrainNoChange("")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ClickableInputField(
                    value = "${state.dateLabel} (${state.dateInput})",
                    label = "Journey Date",
                    placeholder = "Select journey date",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { showDateBottomSheet = true }
                )

                AnimatedVisibility(visible = state.errorMessage != null) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSearch,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching Live Status...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Get Live Status",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Real-Time Tracking",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Live running status is fetched directly from NTES (Indian Railways). Select journey date and search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    if (showTrainBottomSheet) {
        TrainSearchBottomSheet(
            searchQuery = state.selectedTrain,
            filteredTrains = state.filteredTrains,
            onSearchQueryChange = onTrainNoChange,
            onSuggestionSelected = { trainInfo ->
                onSuggestionSelected(trainInfo)
                showTrainBottomSheet = false
            },
            onDismiss = { showTrainBottomSheet = false }
        )
    }

    if (showDateBottomSheet) {
        TrackingDateBottomSheet(
            state = state,
            dateList = dateList,
            onDateSelected = { index ->
                val dateLabel = dateList[index]
                val formattedDate = getFormattedDateForNtes(index - 2)
                onDateOptionSelected(dateLabel, formattedDate)
                showDateBottomSheet = false
            },
            onDismiss = { showDateBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingDateBottomSheet(
    state: TrainTrackingState,
    dateList: Array<String>,
    onDateSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Select Journey Date",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            dateList.forEachIndexed { index, label ->
                val formattedDate = getFormattedDateForNtes(index - 2)
                val isSelected = state.dateLabel == label || state.dateInput == formattedDate

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onDateSelected(index) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Date: $formattedDate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Result step ───────────────────────────────────────────────────────────────

/** Segment representing a stopping station and any non-stopping passing stations after it. */
private data class RouteSegment(
    val stoppingStation: StationStop,
    val passingStations: List<StationStop>
)

private sealed class TrainCurrentPosition {
    data class AtStation(val segmentIndex: Int, val station: StationStop) : TrainCurrentPosition()
    data class InTransit(
        val fromSegmentIndex: Int,
        val fromStation: StationStop,
        val toStation: StationStop
    ) : TrainCurrentPosition()
    data object NotStarted : TrainCurrentPosition()
    data class Completed(val lastStation: StationStop) : TrainCurrentPosition()
}

@Composable
private fun TrainResultStep(
    status: TrainRunningStatus,
    errorMessage: String?,
    onTrackAnother: () -> Unit
) {
    val segments = remember(status.stops) {
        val list = mutableListOf<RouteSegment>()
        var currentStop: StationStop? = null
        val currentPassing = mutableListOf<StationStop>()

        for (stop in status.stops) {
            if (stop.isStop) {
                if (currentStop != null) {
                    list.add(RouteSegment(currentStop, currentPassing.toList()))
                    currentPassing.clear()
                }
                currentStop = stop
            } else {
                if (currentStop != null) currentPassing.add(stop)
            }
        }
        if (currentStop != null) list.add(RouteSegment(currentStop, currentPassing.toList()))
        list
    }

    val currentPosition = remember(segments, status.currentStatus) {
        if (segments.isEmpty()) return@remember TrainCurrentPosition.NotStarted

        // Priority 1: stop with live ISA=true and not yet departed
        val liveAtStationIdx = segments.indexOfFirst {
            it.stoppingStation.isLiveLocation &&
                !it.stoppingStation.liveStatusText.contains("Departed", ignoreCase = true)
        }
        if (liveAtStationIdx != -1)
            return@remember TrainCurrentPosition.AtStation(liveAtStationIdx, segments[liveAtStationIdx].stoppingStation)

        // Priority 2: stop explicitly AT_STATION
        val atStationIdx = segments.indexOfFirst { it.stoppingStation.status == StopStatus.AT_STATION }
        if (atStationIdx != -1)
            return@remember TrainCurrentPosition.AtStation(atStationIdx, segments[atStationIdx].stoppingStation)

        // Priority 3: last stop that is DEPARTED (train is after it, heading to next stop)
        val liveDepartedIdx = segments.indexOfLast {
            it.stoppingStation.isLiveLocation ||
                it.passingStations.any { p -> p.isLiveLocation } ||
                it.stoppingStation.status == StopStatus.DEPARTED
        }
        if (liveDepartedIdx != -1) {
            return@remember if (liveDepartedIdx < segments.lastIndex)
                TrainCurrentPosition.InTransit(
                    fromSegmentIndex = liveDepartedIdx,
                    fromStation = segments[liveDepartedIdx].stoppingStation,
                    toStation = segments[liveDepartedIdx + 1].stoppingStation
                )
            else TrainCurrentPosition.Completed(segments.last().stoppingStation)
        }

        // Priority 4: fall back to currentStatus text matching
        val rawStatusLower = status.currentStatus.lowercase()
        if (rawStatusLower.contains("arrived at") || rawStatusLower.contains("standing at") || rawStatusLower.contains("at station")) {
            val matchedIdx = segments.indexOfFirst { segment ->
                rawStatusLower.contains(segment.stoppingStation.stationCode.lowercase()) ||
                    (segment.stoppingStation.stationName.isNotBlank() &&
                        rawStatusLower.contains(segment.stoppingStation.stationName.lowercase()))
            }
            if (matchedIdx != -1)
                return@remember TrainCurrentPosition.AtStation(matchedIdx, segments[matchedIdx].stoppingStation)
        }

        TrainCurrentPosition.NotStarted
    }

    // Compute which segment index the train is at / just departed from
    val currentSegmentIndex = when (currentPosition) {
        is TrainCurrentPosition.AtStation -> currentPosition.segmentIndex
        is TrainCurrentPosition.InTransit -> currentPosition.fromSegmentIndex
        is TrainCurrentPosition.Completed -> segments.lastIndex
        TrainCurrentPosition.NotStarted   -> -1
    }

    // Journey progress fraction (0f to 1f)
    val journeyProgress = remember(segments, currentSegmentIndex) {
        if (segments.size <= 1 || currentSegmentIndex < 0) 0f
        else (currentSegmentIndex.toFloat() / (segments.size - 1).toFloat()).coerceIn(0f, 1f)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to current position
    LaunchedEffect(currentSegmentIndex) {
        if (currentSegmentIndex >= 0) {
            val targetIndex = 2 + currentSegmentIndex
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .collect { total ->
                    if (total > 0) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                index = targetIndex.coerceAtMost(total - 1),
                                scrollOffset = -60
                            )
                        }
                        return@collect
                    }
                }
        }
    }

    val expandedPassingIndices = remember { mutableStateListOf<Int>() }
    val totalStopsCount   = segments.size
    val totalPassingCount = status.stops.count { !it.isStop }
    var showCoachModalForStop by remember { mutableStateOf<StationStop?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── 1. Train Header Card & Journey Progress Bar ─────────────────────
        item(key = "train_header_card") {
            TrainHeaderCard(
                status = status,
                progress = journeyProgress,
                currentSegmentIndex = currentSegmentIndex,
                totalSegments = segments.size,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // ── 2. Column Headers (Arrival | Station | Departure) ───────────────
        item(key = "route_column_headers") {
            RouteTableColumnHeaders(
                totalStops = totalStopsCount,
                totalPassing = totalPassingCount,
                allExpanded = totalPassingCount > 0 && expandedPassingIndices.size == segments.count { it.passingStations.isNotEmpty() },
                onToggleAllPassing = {
                    val allExp = expandedPassingIndices.size == segments.count { it.passingStations.isNotEmpty() }
                    expandedPassingIndices.clear()
                    if (!allExp) {
                        segments.forEachIndexed { i, s ->
                            if (s.passingStations.isNotEmpty()) expandedPassingIndices.add(i)
                        }
                    }
                }
            )
        }

        // ── 3. Route Segments (Columnar Rows) ───────────────────────────────
        segments.forEachIndexed { index, segment ->
            val isTrainInTransitAfterThis = currentPosition is TrainCurrentPosition.InTransit &&
                currentPosition.fromSegmentIndex == index
            val isLiveStation = (currentPosition is TrainCurrentPosition.AtStation &&
                currentPosition.segmentIndex == index) || segment.stoppingStation.isLiveLocation
            val isDeparted = (currentSegmentIndex >= 0 && index < currentSegmentIndex) ||
                segment.stoppingStation.status == StopStatus.DEPARTED
            val isPassingExpanded = expandedPassingIndices.contains(index)

            item(key = "stop_${segment.stoppingStation.stationCode}_$index") {
                ColumnarStationRow(
                    segment = segment,
                    isFirst = index == 0,
                    isLast = index == segments.lastIndex && !isTrainInTransitAfterThis,
                    isLive = isLiveStation,
                    isDeparted = isDeparted,
                    isPassingExpanded = isPassingExpanded,
                    showBottomTrack = index != segments.lastIndex || isTrainInTransitAfterThis,
                    onTogglePassing = {
                        if (segment.passingStations.isNotEmpty()) {
                            if (isPassingExpanded) expandedPassingIndices.remove(index) else expandedPassingIndices.add(index)
                        }
                    },
                    onShowCoach = { showCoachModalForStop = segment.stoppingStation }
                )
            }

            // ── Inline Live Train Banner on Track ───────────────────────────
            if (isTrainInTransitAfterThis) {
                item(key = "live_transit_banner_$index") {
                    val inTransit = currentPosition as TrainCurrentPosition.InTransit
                    InlineLiveStatusBanner(
                        fromStation = inTransit.fromStation,
                        toStation = inTransit.toStation,
                        currentStatus = status.currentStatus,
                        lastUpdated = status.lastUpdatedOn,
                        delayMins = status.currentDelayMins,
                        isLastBeforeNext = index == segments.lastIndex - 1
                    )
                }
            }
        }

        // ── 4. Error message if any ─────────────────────────────────────────
        if (errorMessage != null) {
            item(key = "error_msg") {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // ── 5. Dynamic Data Disclaimer Card ─────────────────────────────────
        item(key = "footer_disclaimer") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Note: Times marked with (*) are dynamic estimations based on NTES live feed and may change.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 6. Track Another Train Button ───────────────────────────────────
        item(key = "track_another_button") {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Button(
                    onClick = onTrackAnother,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Track Another Train",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Coach Position Dialog
    showCoachModalForStop?.let { stop ->
        CoachPositionDialog(
            stationName = stop.stationName.ifBlank { stop.stationCode },
            platform = stop.platform,
            coaches = stop.coachPositions,
            divyangjanInfo = stop.divyangjanInfo,
            onDismiss = { showCoachModalForStop = null }
        )
    }
}

// ─── Train header card with progress bar ───────────────────────────────────────

@Composable
private fun TrainHeaderCard(
    status: TrainRunningStatus,
    progress: Float,
    currentSegmentIndex: Int,
    totalSegments: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Train Number Badge + Train Name + LIVE badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = status.trainNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.trainName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GreenDot
                    ) {
                        Text(
                            text = "● LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (status.currentDelayMins != null) {
                        DelayPill(delayMins = status.currentDelayMins)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Origin → Destination with distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (status.sourceStationName.isNotBlank()) "${status.sourceStationName} (${status.sourceStation})" else status.sourceStation.ifBlank { "Origin" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    if (status.totalDistance.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${status.totalDistance} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = if (status.destStationName.isNotBlank()) "${status.destStationName} (${status.destStation})" else status.destStation.ifBlank { "Destination" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Start Date & Train Type
            if (status.startDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Start Date: ${status.startDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (status.trainType.isNotBlank()) {
                        Text(
                            text = " • ${status.trainType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Horizontal Journey Progress Bar ─────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                    // Filled progress track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceAtLeast(0.02f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(GreenDot)
                    )
                    // Train icon marker at current progress position
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0.03f, 0.97f)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GreenLive,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = "Train location",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // Progress captions below bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = status.sourceStation,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentSegmentIndex >= 0 && totalSegments > 0) {
                        val pct = (progress * 100).toInt()
                        Text(
                            text = "$pct% completed (Stop ${currentSegmentIndex + 1}/$totalSegments)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = GreenLive
                        )
                    }
                    Text(
                        text = status.destStation,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Classes row if available
            if (status.classes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    status.classes.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { cls ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = cls,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Table column headers (Arrival | Station | Departure) ──────────────────────

@Composable
private fun RouteTableColumnHeaders(
    totalStops: Int,
    totalPassing: Int,
    allExpanded: Boolean,
    onToggleAllPassing: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arrival column label
            Text(
                text = "Arrival",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(74.dp)
            )

            // Station column label + count + expand all
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Station",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " ($totalStops)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (totalPassingCountIsPositive(totalPassing)) {
                    Text(
                        text = if (allExpanded) "Collapse All" else "Expand All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onToggleAllPassing() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Departure column label
            Text(
                text = "Departure",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(74.dp)
            )
        }
    }
}

private fun totalPassingCountIsPositive(count: Int): Boolean = count > 0

// ─── Columnar station row (3-Column Layout) ───────────────────────────────────

@Composable
private fun ColumnarStationRow(
    segment: RouteSegment,
    isFirst: Boolean,
    isLast: Boolean,
    isLive: Boolean,
    isDeparted: Boolean,
    isPassingExpanded: Boolean,
    showBottomTrack: Boolean,
    onTogglePassing: () -> Unit,
    onShowCoach: () -> Unit
) {
    val stop = segment.stoppingStation
    val passingStations = segment.passingStations

    val trackColor = when {
        isLive       -> GreenDot
        isDeparted   -> MaterialTheme.colorScheme.outline
        else         -> MaterialTheme.colorScheme.primary
    }

    val rowBgColor = when {
        isLive     -> GreenDot.copy(alpha = 0.08f)
        isDeparted -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
        else       -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBgColor)
    ) {
        // Main 3-column station row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Left Column: Arrival Times ──────────────────────────────────
            Column(
                modifier = Modifier.width(74.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (stop.scheduledArrival.isNotBlank()) {
                    Text(
                        text = stop.scheduledArrival,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val actArr = stop.actualArrival.ifBlank { stop.scheduledArrival }
                    val isArrDelayed = (stop.delayMinutes != null && stop.delayMinutes > 0) ||
                        (stop.arrivalDelay.isNotBlank() && stop.arrivalDelay != "00:00" && !stop.arrivalDelay.startsWith("00:0"))
                    val arrColor = if (isArrDelayed) MaterialTheme.colorScheme.error else GreenLive

                    Text(
                        text = actArr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = arrColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DelayBadge(delayMins = stop.delayMinutes, delayText = stop.arrivalDelay)
                } else if (isFirst) {
                    Text(
                        text = "Source",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // ── Center Column: Track + Station Info ─────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        val lineX = 10.dp.toPx()
                        val strokeW = 2.dp.toPx()
                        val topY = 0f
                        val dotCenterY = 12.dp.toPx()
                        val bottomY = size.height

                        // Top rail connector
                        if (!isFirst) {
                            drawLine(
                                color = trackColor,
                                start = Offset(lineX, topY),
                                end = Offset(lineX, dotCenterY - 6.dp.toPx()),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                        // Bottom rail connector
                        if (showBottomTrack || passingStations.isNotEmpty()) {
                            drawLine(
                                color = trackColor,
                                start = Offset(lineX, dotCenterY + 6.dp.toPx()),
                                end = Offset(lineX, bottomY),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .padding(start = 0.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Track Node Circle
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .padding(top = 3.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isLive) {
                        Surface(
                            shape = CircleShape,
                            color = GreenDot,
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = "Live",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    } else if (isDeparted) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(GreenDot)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Station Details Column
                Column(modifier = Modifier.weight(1f)) {
                    // Station Name
                    Text(
                        text = stop.stationName.ifBlank { stop.stationCode },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Station Code + Platform + Distance + Halt chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = stop.stationCode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (stop.platform.isNotBlank()) {
                            PlatformBadge(stop.platform)
                        }

                        if (stop.distance.isNotBlank()) {
                            Text(
                                text = stop.distance,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (stop.haltMinutes != null && stop.haltMinutes > 0) {
                            Text(
                                text = "• ${stop.haltMinutes}m halt",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (stop.dayCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            ) {
                                Text(
                                    text = "D${stop.dayCount + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }

                    // Coach Position chip & Passing Station toggle
                    if (stop.coachPositions.isNotEmpty() || passingStations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (stop.coachPositions.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { onShowCoach() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Train,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Coach >>",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            if (passingStations.isNotEmpty()) {
                                Text(
                                    text = if (isPassingExpanded) "Hide passing" else "+${passingStations.size} passing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onTogglePassing() }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Right Column: Departure Times ───────────────────────────────
            Column(
                modifier = Modifier.width(74.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (stop.scheduledDeparture.isNotBlank()) {
                    Text(
                        text = stop.scheduledDeparture,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val actDep = stop.actualDeparture.ifBlank { stop.scheduledDeparture }
                    val isDepDelayed = (stop.delayMinutes != null && stop.delayMinutes > 0) ||
                        (stop.departureDelay.isNotBlank() && stop.departureDelay != "00:00" && !stop.departureDelay.startsWith("00:0"))
                    val depColor = if (isDepDelayed) MaterialTheme.colorScheme.error else GreenLive

                    Text(
                        text = actDep,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = depColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DelayBadge(delayMins = stop.delayMinutes, delayText = stop.departureDelay)
                } else if (isLast) {
                    Text(
                        text = "Dest",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // ── Passing Stations List (Indented under station) ───────────────────
        if (passingStations.isNotEmpty()) {
            AnimatedVisibility(
                visible = isPassingExpanded,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 74.dp + 6.dp, end = 12.dp, bottom = 4.dp)
                ) {
                    passingStations.forEach { passing ->
                        PassingStationRow(passing = passing)
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

// ─── Inline live train status banner (On Track) ────────────────────────────────

@Composable
private fun InlineLiveStatusBanner(
    fromStation: StationStop,
    toStation: StationStop,
    currentStatus: String,
    lastUpdated: String,
    delayMins: Int?,
    isLastBeforeNext: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = AmberLight,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Train Badge
            Box(
                modifier = Modifier
                    .width(74.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = CircleShape,
                    color = GreenLive,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = "Live Train Location",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Live Text & Upcoming Station
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentStatus.ifBlank { "Departed from ${fromStation.stationCode} (${fromStation.stationName})" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF4E2800)
                )

                if (toStation.stationCode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Upcoming: ${toStation.stationName.ifBlank { toStation.stationCode }} (${toStation.stationCode})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF6D3C00)
                    )
                }

                if (lastUpdated.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last updated: $lastUpdated",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFF8D5000)
                    )
                }
            }

            // Delay Pill on right
            if (delayMins != null) {
                Spacer(modifier = Modifier.width(6.dp))
                DelayPill(delayMins = delayMins)
            }
        }
    }
}

// ─── Compact passing station row ───────────────────────────────────────────────

@Composable
private fun PassingStationRow(passing: StationStop) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small dash/circle for passing track
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (passing.isLiveLocation) GreenDot else MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Station Code + Name
        Text(
            text = passing.stationCode,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (passing.stationName != passing.stationCode && passing.stationName.isNotBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "– ${passing.stationName}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Distance & Passing time
        if (passing.distance.isNotBlank()) {
            Text(
                text = passing.distance,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        val passTime = passing.scheduledArrival.ifBlank { passing.scheduledDeparture }
        if (passTime.isNotBlank()) {
            Text(
                text = passTime,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Non-wrapping Delay badge ──────────────────────────────────────────────────

@Composable
private fun DelayBadge(delayMins: Int?, delayText: String) {
    val isDelayed = (delayMins != null && delayMins > 0) ||
        (delayText.isNotBlank() && delayText != "00:00" && !delayText.startsWith("00:0"))

    val (bgColor, fgColor, text) = when {
        !isDelayed -> Triple(GreenDot, Color.White, "On Time")
        delayMins != null && delayMins > 0 -> {
            if (delayMins < 60) Triple(RedDelay, Color.White, "${delayMins} Min")
            else Triple(RedDelay, Color.White, "${delayMins / 60}h ${delayMins % 60}m")
        }
        delayText.isNotBlank() -> Triple(RedDelay, Color.White, delayText)
        else -> Triple(GreenDot, Color.White, "On Time")
    }

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            color = fgColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// ─── Platform badge ────────────────────────────────────────────────────────────

@Composable
private fun PlatformBadge(platform: String) {
    if (platform.isBlank()) return
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFFFF8F00).copy(alpha = 0.18f)
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color(0xFFE65100),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// ─── Delay pill (header) ───────────────────────────────────────────────────────

@Composable
private fun DelayPill(delayMins: Int) {
    val (bgColor, fgColor, label) = when {
        delayMins <= 0  -> Triple(GreenDot, Color.White, "On Time")
        delayMins < 10  -> Triple(Color(0xFFFFC107), Color(0xFF3E2000), "${delayMins}m late")
        delayMins < 60  -> Triple(AmberDelay, Color.White, "${delayMins}m late")
        else            -> Triple(RedDelay, Color.White, "${delayMins / 60}h ${delayMins % 60}m late")
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = fgColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        )
    }
}

// ─── Coach position dialog ─────────────────────────────────────────────────────

@Composable
private fun CoachPositionDialog(
    stationName: String,
    platform: String,
    coaches: List<com.vacart.model.CoachPositionInfo>,
    divyangjanInfo: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Coach Position",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$stationName ${if (platform.isNotBlank()) "• $platform" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Engine ➔ Tail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    coaches.forEach { coach ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = coach.coachType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = coach.coachName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "#${coach.positionIndex}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                if (divyangjanInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "♿ $divyangjanInfo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
