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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vacart.model.StationStop
import com.vacart.model.StopStatus
import com.vacart.model.TrainRunningStatus
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
    onDateOptionSelected: (String, String) -> Unit,
    onSearch: () -> Unit
) {
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
                    text = "Enter train number and select journey date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = state.trainNoInput,
                    onValueChange = { if (it.length <= 10) onTrainNoChange(it) },
                    label = { Text("Train Number / Name") },
                    placeholder = { Text("e.g. 12626 or 00112") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (state.trainNoInput.isNotEmpty()) {
                            IconButton(onClick = { onTrainNoChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

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

@Composable
private fun ClickableInputField(
    value: String,
    label: String,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
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

    // Count the fixed header lazy items that sit above the segment list
    val headerItemCount = remember(status) {
        var count = 1  // TrainHeaderCard
        count++        // Spacer after header
        if (status.sourceStation.isNotBlank() || status.destStation.isNotBlank()) {
            count++    // RouteSummaryBar
            count++    // Spacer after summary
        }
        if (status.currentStatus.isNotBlank()) {
            count++    // CurrentStatusCard
            count++    // Spacer after status card
        }
        count++        // Section header row
        count
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll once the list has been laid out and we know where the train is.
    // snapshotFlow waits until totalItemsCount > 0 so we never scroll to index 0 by mistake.
    LaunchedEffect(currentSegmentIndex) {
        if (currentSegmentIndex >= 0) {
            val targetIndex = headerItemCount + currentSegmentIndex
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .collect { total ->
                    if (total > 0) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                index = targetIndex.coerceAtMost(total - 1),
                                scrollOffset = -120
                            )
                        }
                        return@collect
                    }
                }
        }
    }

    val expandedPassingIndices = remember { mutableStateListOf<Int>() }
    val expandedStationIndices = remember { mutableStateListOf<Int>() }
    val totalStopsCount   = segments.size
    val totalPassingCount = status.stops.count { !it.isStop }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Train header card ──────────────────────────────────────────────
        item { TrainHeaderCard(status = status) }
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // ── Route summary bar (origin → destination) ───────────────────────
        if (status.sourceStation.isNotBlank() || status.destStation.isNotBlank()) {
            item { RouteSummaryBar(status = status) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // ── Current status card ────────────────────────────────────────────
        if (status.currentStatus.isNotBlank()) {
            item {
                CurrentStatusCard(
                    currentStatus = status.currentStatus,
                    lastUpdatedOn = status.lastUpdatedOn,
                    delayMins     = status.currentDelayMins
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // ── Route overview header ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stops ($totalStopsCount)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (totalPassingCount > 0) {
                        Text(
                            text = " • $totalPassingCount passing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (totalPassingCount > 0) {
                    val allExpanded = expandedPassingIndices.size == segments.count { it.passingStations.isNotEmpty() }
                    TextButton(
                        onClick = {
                            if (allExpanded) {
                                expandedPassingIndices.clear()
                            } else {
                                expandedPassingIndices.clear()
                                segments.forEachIndexed { i, s ->
                                    if (s.passingStations.isNotEmpty()) expandedPassingIndices.add(i)
                                }
                            }
                        }
                    ) {
                        Text(if (allExpanded) "Collapse All" else "Expand All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // ── Route segments ─────────────────────────────────────────────────
        itemsIndexed(segments) { index, segment ->
            val isPassingExpanded  = expandedPassingIndices.contains(index)
            val isStationExpanded  = expandedStationIndices.contains(index)
            val isFirst            = index == 0
            val isLast             = index == segments.lastIndex
            val isCurrentStation   = (currentPosition is TrainCurrentPosition.AtStation &&
                currentPosition.segmentIndex == index) || segment.stoppingStation.isLiveLocation
            val isTrainInTransitAfterThis = currentPosition is TrainCurrentPosition.InTransit &&
                currentPosition.fromSegmentIndex == index

            RouteSegmentItem(
                segment           = segment,
                isFirst           = isFirst,
                isLast            = isLast && !isTrainInTransitAfterThis,
                isCurrentStation  = isCurrentStation,
                isPassingExpanded = isPassingExpanded,
                isStationExpanded = isStationExpanded,
                showBottomLine    = !isLast || isTrainInTransitAfterThis,
                onTogglePassing   = {
                    if (segment.passingStations.isNotEmpty()) {
                        if (isPassingExpanded) expandedPassingIndices.remove(index) else expandedPassingIndices.add(index)
                    }
                },
                onToggleStation   = {
                    if (isStationExpanded) expandedStationIndices.remove(index) else expandedStationIndices.add(index)
                }
            )

            if (isTrainInTransitAfterThis) {
                InTransitTimelineItem(
                    fromStation = segment.stoppingStation,
                    toStation   = (currentPosition as TrainCurrentPosition.InTransit).toStation,
                    delayMins   = status.currentDelayMins,
                    isLastBeforeNext = true
                )
            }
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTrackAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Track Another Train",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Header card ───────────────────────────────────────────────────────────────

@Composable
private fun TrainHeaderCard(status: TrainRunningStatus) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.trainName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Train #${status.trainNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (status.trainType.isNotBlank()) {
                            Text(
                                text = " • ${status.trainType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    // LIVE badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GreenDot
                    ) {
                        Text(
                            text = "● LIVE",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    // Delay pill
                    if (status.currentDelayMins != null && status.currentDelayMins > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DelayPill(delayMins = status.currentDelayMins)
                    }
                }
            }

            // Classes row
            if (status.classes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    status.classes.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { cls ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = cls,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (status.startDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Date: ${status.startDate}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Route summary bar ─────────────────────────────────────────────────────────

@Composable
private fun RouteSummaryBar(status: TrainRunningStatus) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = status.sourceStation.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (status.sourceStationName.isNotBlank()) {
                    Text(
                        text = status.sourceStationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Arrow + distance
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                if (status.totalDistance.isNotBlank()) {
                    Text(
                        text = "${status.totalDistance} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            // Destination
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = status.destStation.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (status.destStationName.isNotBlank()) {
                    Text(
                        text = status.destStationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Current status card ───────────────────────────────────────────────────────

@Composable
private fun CurrentStatusCard(
    currentStatus: String,
    lastUpdatedOn: String,
    delayMins: Int?
) {
    val isDelayed = delayMins != null && delayMins > 0
    val cardColor = if (isDelayed)
        AmberLight
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    val onCardColor = if (isDelayed)
        Color(0xFF4E2800)
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isDelayed) AmberDelay else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = onCardColor
                    )
                }
                if (delayMins != null) DelayPill(delayMins = delayMins)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentStatus,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = onCardColor
            )
            if (lastUpdatedOn.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = onCardColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Last updated: $lastUpdatedOn",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─── Delay pill ────────────────────────────────────────────────────────────────

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
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Route segment item ────────────────────────────────────────────────────────
// Timeline uses an IntrinsicSize.Min row so the vertical line stretches to match
// whatever the card height is – this keeps the line perfectly connected.

@Composable
private fun RouteSegmentItem(
    segment: RouteSegment,
    isFirst: Boolean,
    isLast: Boolean,
    isCurrentStation: Boolean = false,
    isPassingExpanded: Boolean,
    isStationExpanded: Boolean,
    showBottomLine: Boolean,
    onTogglePassing: () -> Unit,
    onToggleStation: () -> Unit
) {
    val stop = segment.stoppingStation
    val passingStations = segment.passingStations
    var showCoachModal by remember { mutableStateOf(false) }

    val isLive = isCurrentStation || stop.isLiveLocation || stop.status == StopStatus.AT_STATION

    val dotColor = when {
        isLive                             -> GreenDot
        stop.status == StopStatus.DEPARTED -> MaterialTheme.colorScheme.primary
        stop.status == StopStatus.UPCOMING -> MaterialTheme.colorScheme.outline
        else                               -> MaterialTheme.colorScheme.outlineVariant
    }
    val lineColor = when {
        isLive                             -> MaterialTheme.colorScheme.primary
        stop.status == StopStatus.DEPARTED -> MaterialTheme.colorScheme.primary
        else                               -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 6.dp)
    ) {
        // ── Main stop row (timeline dot + card) ───────────────────────────
        // height(IntrinsicSize.Min) lets the timeline Column know the Row's
        // height so weight(1f) on the bottom connector line fills it exactly.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            // ── Timeline column (dot + connecting lines) ──────────────────
            // The bottom line uses weight(1f) to stretch from dot to the
            // card's bottom edge — keeping the timeline perfectly connected.
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .padding(top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top connector line (not for first station)
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(10.dp)
                            .background(lineColor)
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Dot / train icon
                if (isLive) {
                    Surface(
                        shape = CircleShape,
                        color = GreenDot,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "Current Train Location",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                // Bottom connector line — fillMaxHeight() works because the
                // parent Row is measured with IntrinsicSize.Min, so this box
                // stretches to fill the card height and keeps the line connected.
                if (showBottomLine || passingStations.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(lineColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── Station card ──────────────────────────────────────────────
            CompactStationCard(
                stop              = stop,
                passingStations   = passingStations,
                isLive            = isLive,
                isPassingExpanded = isPassingExpanded,
                isStationExpanded = isStationExpanded,
                onTogglePassing   = onTogglePassing,
                onToggleStation   = onToggleStation,
                onShowCoach       = { showCoachModal = true },
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (showBottomLine || passingStations.isNotEmpty()) 0.dp else 2.dp)
            )
        }

        // ── Passing stations (indented, below the card) ───────────────────
        if (passingStations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
            ) {
                // Expand/collapse toggle row for passing stations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePassing() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small connecting vertical dash on left
                    Icon(
                        imageVector = if (isPassingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPassingExpanded) "Hide ${passingStations.size} passing stations"
                               else "+ ${passingStations.size} passing stations",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = isPassingExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit  = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        passingStations.forEach { passing ->
                            PassingStationItem(passing = passing)
                        }
                    }
                }
            }
        }
    }

    if (showCoachModal && stop.coachPositions.isNotEmpty()) {
        CoachPositionDialog(
            stationName    = stop.stationName.ifBlank { stop.stationCode },
            platform       = stop.platform,
            coaches        = stop.coachPositions,
            divyangjanInfo = stop.divyangjanInfo,
            onDismiss      = { showCoachModal = false }
        )
    }
}

// ─── Compact station card (collapsed + expandable) ─────────────────────────────

@Composable
private fun CompactStationCard(
    stop: StationStop,
    passingStations: List<StationStop>,
    isLive: Boolean,
    isPassingExpanded: Boolean,
    isStationExpanded: Boolean,
    onTogglePassing: () -> Unit,
    onToggleStation: () -> Unit,
    onShowCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isStationExpanded) 180f else 0f,
        label = "chevron"
    )

    // Compute a compact time string for collapsed view
    val primaryTime = when {
        stop.scheduledArrival.isNotBlank()   -> stop.scheduledArrival
        stop.scheduledDeparture.isNotBlank() -> stop.scheduledDeparture
        else                                 -> ""
    }
    val hasTimeInfo = stop.scheduledArrival.isNotBlank() || stop.scheduledDeparture.isNotBlank() ||
        stop.actualArrival.isNotBlank() || stop.actualDeparture.isNotBlank()
    val hasExpandableInfo = hasTimeInfo || stop.distance.isNotBlank() || stop.haltMinutes != null ||
        stop.coachPositions.isNotEmpty() || stop.liveStatusText.isNotBlank()

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isLive                             -> GreenDot.copy(alpha = 0.10f)
                stop.status == StopStatus.DEPARTED -> MaterialTheme.colorScheme.surfaceContainerLow
                else                               -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isLive) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {

            // ── Collapsed header row ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasExpandableInfo) { onToggleStation() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station code + name
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = stop.stationCode,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (stop.platform.isNotBlank()) {
                            PlatformBadge(stop.platform)
                        }
                        if (stop.dayCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            ) {
                                Text(
                                    text = "D${stop.dayCount + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (stop.stationName != stop.stationCode && stop.stationName.isNotBlank()) {
                        Text(
                            text = stop.stationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side: time + live badge / chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Show scheduled time compactly in collapsed view
                    if (!isStationExpanded && primaryTime.isNotBlank()) {
                        val actualTime = when {
                            stop.scheduledArrival.isNotBlank() && stop.actualArrival.isNotBlank() -> stop.actualArrival
                            stop.scheduledDeparture.isNotBlank() && stop.actualDeparture.isNotBlank() -> stop.actualDeparture
                            else -> ""
                        }
                        val isDelayed = stop.delayMinutes != null && stop.delayMinutes > 0
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = primaryTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (actualTime.isNotBlank() && actualTime != primaryTime) {
                                Text(
                                    text = actualTime,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDelayed) MaterialTheme.colorScheme.error else GreenLive
                                )
                            }
                        }
                    }

                    if (isLive) {
                        Surface(shape = RoundedCornerShape(50), color = GreenDot) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "HERE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (hasExpandableInfo) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isStationExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            // ── Expanded detail section ───────────────────────────────────
            AnimatedVisibility(
                visible = isStationExpanded,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // Live event banner
                    if (stop.liveStatusText.isNotBlank() || stop.updatedOn.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GreenLive,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (stop.liveStatusText.isNotBlank()) {
                                        Text(
                                            text = stop.liveStatusText,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GreenLive
                                        )
                                    }
                                    if (stop.updatedOn.isNotBlank()) {
                                        Text(
                                            text = "Updated: ${stop.updatedOn}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF388E3C)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Time blocks (arrival + departure) with delay diff
                    val hasArrival   = stop.scheduledArrival.isNotBlank() || stop.actualArrival.isNotBlank()
                    val hasDeparture = stop.scheduledDeparture.isNotBlank() || stop.actualDeparture.isNotBlank()

                    if (hasArrival || hasDeparture) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (hasArrival) {
                                TimeBlock(
                                    label     = "Arrival",
                                    scheduled = stop.scheduledArrival,
                                    actual    = stop.actualArrival,
                                    delay     = stop.arrivalDelay,
                                    delayMins = stop.delayMinutes,
                                    modifier  = Modifier.weight(1f)
                                )
                            }
                            if (hasDeparture) {
                                TimeBlock(
                                    label     = "Departure",
                                    scheduled = stop.scheduledDeparture,
                                    actual    = stop.actualDeparture,
                                    delay     = stop.departureDelay,
                                    delayMins = stop.delayMinutes,
                                    modifier  = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Distance + halt time + coach button
                    val hasBottomInfo = stop.distance.isNotBlank() ||
                        stop.haltMinutes != null ||
                        stop.coachPositions.isNotEmpty()

                    if (hasBottomInfo) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stop.distance.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Straighten,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = stop.distance,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (stop.haltMinutes != null && stop.haltMinutes > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${stop.haltMinutes}m halt",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (stop.coachPositions.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onShowCoach,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Coach",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Passing station row ────────────────────────────────────────────────────────

@Composable
private fun PassingStationItem(passing: StationStop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (passing.isLiveLocation) GreenLight
                             else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (passing.isLiveLocation) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (passing.isLiveLocation) GreenDot else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = passing.stationCode,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (passing.stationName != passing.stationCode && passing.stationName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "– ${passing.stationName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (passing.distance.isNotBlank()) {
                        Text(
                            text = passing.distance,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (passing.dayCount > 0) {
                        Text(
                            text = "D${passing.dayCount + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (passing.liveStatusText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = passing.liveStatusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = GreenLive
                )
            }
        }
    }
}

// ─── Time block ─────────────────────────────────────────────────────────────────

@Composable
private fun TimeBlock(
    label: String,
    scheduled: String,
    actual: String,
    delay: String = "",
    delayMins: Int? = null,
    modifier: Modifier = Modifier
) {
    val isDelayed = (delayMins != null && delayMins > 0) ||
                    (delay.isNotBlank() && delay != "00:00" && !delay.startsWith("00:0"))
    val actualColor = when {
        actual.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
        isDelayed        -> MaterialTheme.colorScheme.error
        else             -> GreenLive
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        if (scheduled.isNotBlank()) {
            Text(
                text = "Sch: $scheduled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actual.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Act: $actual",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = actualColor
                )
                if (delay.isNotBlank()) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDelayed) MaterialTheme.colorScheme.errorContainer
                                else GreenDot.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = delay,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDelayed) MaterialTheme.colorScheme.onErrorContainer else GreenLive,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        if (actual.isBlank() && scheduled.isBlank() && delay.isBlank()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ─── Platform badge ────────────────────────────────────────────────────────────

@Composable
private fun PlatformBadge(platform: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

// ─── In-transit timeline item ──────────────────────────────────────────────────

@Composable
private fun InTransitTimelineItem(
    fromStation: StationStop,
    toStation: StationStop,
    delayMins: Int?,
    isLastBeforeNext: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column with connecting lines + animated train icon
        Column(
            modifier = Modifier.width(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = "Train in transit",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (isLastBeforeNext) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In Transit 🚆",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (delayMins != null) DelayPill(delayMins = delayMins)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${fromStation.stationCode} → ${toStation.stationCode}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                val nextActual = toStation.actualArrival.ifBlank { toStation.scheduledArrival }
                if (nextActual.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "ETA ${toStation.stationCode}: $nextActual",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
