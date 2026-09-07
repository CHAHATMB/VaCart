package com.vacart.presentation.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vacart.model.Station
import com.vacart.presentation.home.HomeEvent
import com.vacart.presentation.home.HomeState
import com.vacart.presentation.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainSchedule(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val state by homeViewModel.state.collectAsState()
    val event = homeViewModel::onEvent

    LaunchedEffect(state.trainNumber) {
        if (state.stationList == null || state.stationList?.trainNumber != state.trainNumber) {
            event(HomeEvent.fetchStationList(state.trainNumber))
        }
    }

    val primaryHeaderBlue = Color(0xFF3F51B5)
    val textBlue = Color(0xFF3F51B5)
    val greenDotColor = Color(0xFF4CAF50)
    val blueDotColor = Color(0xFF2196F3)
    val redDotColor = Color(0xFFF44336)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (!state.stationList?.trainName.isNullOrEmpty())
                            "Train Schedule - ${state.stationList?.trainName}"
                        else "Train Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryHeaderBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Legend Header Section
            LegendSection(
                greenColor = greenDotColor,
                blueColor = blueDotColor,
                redColor = redDotColor
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

            // Table Header Row
            TableHeaderRow(textBlue = textBlue)

            HorizontalDivider(color = textBlue, thickness = 1.5.dp)

            // Content Section (Loading / Error / List)
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = primaryHeaderBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fetching train schedule...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.stationList?.stationList.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No station schedule found for train ${state.trainNumber}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { event(HomeEvent.fetchStationList(state.trainNumber)) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            } else {
                val stationList = state.stationList?.stationList ?: emptyList()
                val boardingStationCode = state.boardingStation.ifEmpty { state.stationList?.stationFrom.orEmpty() }
                val chartingStationCode = state.trainComposition?.chartStatusResponseDto?.remoteStationCode
                    ?: state.stationList?.stationFrom.orEmpty()

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(stationList) { index, station ->
                        StationScheduleRow(
                            station = station,
                            boardingStationCode = boardingStationCode,
                            chartingStationCode = chartingStationCode,
                            greenColor = greenDotColor,
                            blueColor = blueDotColor,
                            redColor = redDotColor
                        )
                        HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendSection(
    greenColor: Color,
    blueColor: Color,
    redColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(label = "Boarding Station", dotColor = greenColor)
                LegendItem(label = "Charting Station", dotColor = blueColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LegendItem(label = "Online Booking Not Available", dotColor = redColor)
        }
    }
}

@Composable
fun LegendItem(label: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = dotColor, shape = CircleShape)
        )
    }
}

@Composable
fun TableHeaderRow(textBlue: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Station",
                modifier = Modifier.weight(2.2f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textBlue,
                    fontSize = 15.sp
                )
            )
            Text(
                text = "Day",
                modifier = Modifier.weight(0.7f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textBlue,
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Arrival",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textBlue,
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Departure",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textBlue,
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StationScheduleRow(
    station: Station,
    boardingStationCode: String,
    chartingStationCode: String,
    greenColor: Color,
    blueColor: Color,
    redColor: Color
) {
    val isBoarding = station.stationCode.equals(boardingStationCode, ignoreCase = true)
    val isCharting = station.stationCode.equals(chartingStationCode, ignoreCase = true)
    val isBookingDisabled = station.boardingDisabled.equals("true", ignoreCase = true) ||
            (station.status != null && !station.status.equals("ACTIVE", ignoreCase = true))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Station Name & Code with dots
        Row(
            modifier = Modifier.weight(2.2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${station.stationName ?: ""} (${station.stationCode ?: ""})",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Status Dots next to station code
            Row(modifier = Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isBoarding) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(8.dp)
                            .background(color = greenColor, shape = CircleShape)
                    )
                }
                if (isCharting) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(8.dp)
                            .background(color = blueColor, shape = CircleShape)
                    )
                }
                if (isBookingDisabled) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(8.dp)
                            .background(color = redColor, shape = CircleShape)
                    )
                }
            }
        }

        // Day Count
        Text(
            text = station.dayCount ?: "1",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Arrival Time
        Text(
            text = if (station.arrivalTime.isNullOrBlank()) "--" else station.arrivalTime,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Departure Time
        Text(
            text = if (station.departureTime.isNullOrBlank()) "--" else station.departureTime,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
