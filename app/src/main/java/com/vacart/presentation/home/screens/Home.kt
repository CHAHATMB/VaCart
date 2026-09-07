package com.vacart.presentation.home.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vacart.model.TrainInfo
import com.vacart.navigation.Routes
import com.vacart.presentation.common.ClickableInputField
import com.vacart.presentation.common.TrainSearchAutoCompleteField
import com.vacart.presentation.common.TrainSearchBottomSheet
import com.vacart.presentation.home.HomeEvent
import com.vacart.presentation.home.HomeState
import com.vacart.presentation.home.HomeViewModel
import com.vacart.roomdatabase.SearchEntity
import com.vacart.util.FeatureFlags
import com.vacart.util.getDateBasedOnOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()

    val dateList = arrayOf("2 days ago", "Yesterday", "Today", "Tomorrow")
    var showError by remember { mutableStateOf(false) }

    var showTrainBottomSheet by remember { mutableStateOf(false) }
    var showDateBottomSheet by remember { mutableStateOf(false) }

    val recentSearches by homeViewModel.recentSearches.collectAsState(emptyList())

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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VaCart",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Journey Detail Section Card
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Journey Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                                    IconButton(onClick = { event(HomeEvent.updateTrainNumber("")) }) {
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
                            isError = showError && state.selectedTrain.isEmpty(),
                            onClick = {
                                showError = false
                                showTrainBottomSheet = true
                            }
                        )
                    } else {
                        TrainSearchAutoCompleteField(
                            searchQuery = state.selectedTrain,
                            filteredTrains = state.filteredTrains,
                            showError = showError && state.selectedTrain.isEmpty(),
                            onValueChange = { input ->
                                showError = false
                                event(HomeEvent.updateTrainNumber(input))
                            },
                            onSuggestionSelected = { trainInfo ->
                                showError = false
                                event(HomeEvent.selectTrainInfo(trainInfo))
                            },
                            onClearInput = {
                                event(HomeEvent.updateTrainNumber(""))
                            }
                        )
                    }

                    if (showError && state.selectedTrain.isEmpty()) {
                        Text(
                            text = "Train number cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Selection Field (Bottom Sheet vs Dropdown Feature Flag)
                    if (FeatureFlags.isBottomSheetSearchEnabled) {
                        ClickableInputField(
                            value = state.selectedDateString,
                            label = "Journey Date",
                            placeholder = "Select journey date",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            isError = showError && state.journeyDate.isEmpty(),
                            onClick = {
                                showError = false
                                showDateBottomSheet = true
                            }
                        )
                    } else {
                        DropDownMenuField(state = state, dateList = dateList) { sT ->
                            showError = false
                            state.journeyDate = getDateBasedOnOffset(sT - 2)
                            event(HomeEvent.updateSelectDate(dateList[sT]))
                        }
                    }

                    if (showError && state.journeyDate.isEmpty()) {
                        Text(
                            text = "Date cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Get Chart Button
                    Button(
                        onClick = {
                            if (state.selectedTrain.isEmpty() || state.journeyDate.isEmpty()) {
                                showError = true
                            } else {
                                showError = false
                                homeViewModel.saveSearch(state.selectedTrain, state.journeyDate)
                                navController.navigate(Routes.VacancyChart.routes)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Get Vacancy Chart",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Recent Searches Section
            if (recentSearches.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Searches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn {
                            items(recentSearches) { search ->
                                RecentSearchItem(search = search, onItemClick = {
                                    state.selectedTrain = search.trainNumber
                                    state.journeyDate = search.journeyDate
                                    event(HomeEvent.updateTrainNumber(search.trainNumber))
                                    event(HomeEvent.updateSelectDate(search.journeyDate))
                                })
                            }
                        }
                    }
                }
            }
        }

        // Train Search Bottom Sheet
        if (showTrainBottomSheet) {
            TrainSearchBottomSheet(
                searchQuery = state.selectedTrain,
                filteredTrains = state.filteredTrains,
                onSearchQueryChange = { input ->
                    event(HomeEvent.updateTrainNumber(input))
                },
                onSuggestionSelected = { trainInfo ->
                    event(HomeEvent.selectTrainInfo(trainInfo))
                    showTrainBottomSheet = false
                },
                onDismiss = {
                    showTrainBottomSheet = false
                }
            )
        }

        // Journey Date Selection Bottom Sheet
        if (showDateBottomSheet) {
            DateSelectionBottomSheet(
                state = state,
                dateList = dateList,
                onDateSelected = { index ->
                    state.journeyDate = getDateBasedOnOffset(index - 2)
                    event(HomeEvent.updateSelectDate(dateList[index]))
                    showDateBottomSheet = false
                },
                onDismiss = {
                    showDateBottomSheet = false
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionBottomSheet(
    state: HomeState,
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

            dateList.forEachIndexed { index, dateLabel ->
                val dateValue = getDateBasedOnOffset(index - 2)
                val isSelected = state.selectedDateString == dateLabel

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
                    onClick = {
                        onDateSelected(index)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Date: $dateValue",
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



@Composable
fun RecentSearchItem(search: SearchEntity, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Train ${search.trainNumber}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Date: ${search.journeyDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownMenuField(
    state: HomeState,
    dateList: Array<String>,
    onChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = state.selectedDateString,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Journey Date") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dateList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(text = item, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onChange(index)
                    }
                )
            }
        }
    }
}