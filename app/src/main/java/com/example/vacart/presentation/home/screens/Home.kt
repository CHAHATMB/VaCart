package com.example.vacart.presentation.home.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vacart.util.getDateBasedOnOffset
import androidx.navigation.NavController
import com.example.vacart.navigation.Routes
import com.example.vacart.presentation.home.HomeEvent
import com.example.vacart.presentation.home.HomeState
import com.example.vacart.presentation.home.HomeViewModel
import com.example.vacart.roomdatabase.SearchEntity

@Composable
fun Home(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()

    val dateList = arrayOf("2 days ago", "Yesterday", "Today", "Tomorrow")
    var showError by remember { mutableStateOf(false) } // State to track error

    // Collect recent searches from ViewModel
    val recentSearches by homeViewModel.recentSearches.collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VaCart",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                backgroundColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Journey Detail Section
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Journey Detail",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Train Number Input Field
                    TextField(
                        value = state.selectedTrain,
                        onValueChange = {
                            showError = false
                            state.selectedTrain = it
                            event(HomeEvent.updateTrainNumber(it))
                        },
                        label = { Text(text = "Train Number") },
                        isError = showError && state.selectedTrain.isEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (showError && state.selectedTrain.isEmpty()) {
                        Text(
                            text = "Train number cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Selection Dropdown
                    DropDownMenuField(state = state, dateList = dateList) { sT ->
                        showError = false
                        state.journeyDate = getDateBasedOnOffset(sT - 2)
                        event(HomeEvent.updateSelectDate(dateList[sT]))
                    }
                    if (showError && state.journeyDate.isEmpty()) {
                        Text(
                            text = "Date cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
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
                    ) {
                        Text(text = "Get Chart")
                    }
                }
            }

//            Spacer(modifier = Modifier.height(8.dp))

            // Recent Searches Section
            if (recentSearches.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Recent Searches",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn {
                            items(recentSearches) { search ->
                                RecentSearchItem(search = search, onItemClick = {
                                    // Update UI with recent search data
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
    }
}


@Composable
fun RecentSearches(recentSearches: List<SearchEntity>, event: (HomeEvent) -> Unit,) {
    if (recentSearches.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
            )


            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(recentSearches) { search ->
                    RecentSearchItem(search = search, onItemClick = {
                        // Update UI with recent search data
                        event(HomeEvent.updateTrainNumber(search.trainNumber))
                        event(HomeEvent.updateSelectDate(search.journeyDate))
                    })
                }
            }
        }
    }
}
@Composable
fun RecentSearchItem(search: SearchEntity,  onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh, // Replace with your history icon
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Train: ${search.trainNumber}, Date: ${search.journeyDate}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun DropDownMenuField(
    state: HomeState,
    dateList: Array<String>,
    onChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var previousSelectedDate by remember {
        mutableIntStateOf(2)
    }
    val source = remember {
        MutableInteractionSource()
    } .also { interactionSource ->
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                if (it is PressInteraction.Release) {
                    expanded = !expanded
                }
            }
        }
    }
    // Need to wrap in box so that drop down menu appear below TextField
    Box() {
        TextField(
            value = state.selectedDateString,
            readOnly = true,
            onValueChange = {
//            onChange(it)
                selectedText = it
                expanded = true

            },
            label = { Text(text = "Date") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        expanded = !expanded
                        Log.d("VaChart", "logning - ${expanded}")
                    }
                ) {
                    if (!expanded) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = ""
                        )

                    } else {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = ""
                        )
                    }

                }
            },
            interactionSource = source
        )

        DropdownMenu(
            modifier = Modifier
                .width(200.dp)
                .wrapContentSize(),
            expanded = expanded,
            onDismissRequest = {
                // dismiss the dropdown and select today by default
                selectedText = dateList[previousSelectedDate]
                expanded = false
                onChange(previousSelectedDate)
            }
        ) {
            dateList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        selectedText = item
                        expanded = false
                        Log.d("VaChart", "$item selected")
                        onChange(index)
                        previousSelectedDate = index
                    }
                )
            }
        }

    }
}