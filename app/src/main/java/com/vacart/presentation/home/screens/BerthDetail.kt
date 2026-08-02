package com.vacart.presentation.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vacart.model.Vbd
import com.vacart.presentation.home.HomeEvent
import com.vacart.presentation.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerthDetail(navController: NavController, homeViewModel: HomeViewModel = hiltViewModel()) {

    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()
    val berthDetails: List<Vbd> = state.vacantBerthList
    val searchQuery: String = state.searchQuery

    LaunchedEffect(state.selectedClassCode) {
        event(HomeEvent.getVacantBerth())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Vacant Berth Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Input Field
            SearchBar(event = event, searchQuery = searchQuery)

            Spacer(modifier = Modifier.height(16.dp))

            // Table Header Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeader("From", 1.2f) { event(HomeEvent.sortByColumn("fromStation")) }
                    TableHeader("To", 1.2f) { event(HomeEvent.sortByColumn("toStation")) }
                    TableHeader("Coach", 0.9f) { event(HomeEvent.sortByColumn("coach")) }
                    TableHeader("Berth No", 1.0f)
                    TableHeader("Type", 0.8f)
                }
            }

            // Vacant Berths List
            val filteredData = berthDetails.filter {
                it.from.contains(searchQuery, ignoreCase = true) ||
                        it.to.contains(searchQuery, ignoreCase = true) ||
                        it.coachName.contains(searchQuery, ignoreCase = true)
            }

            if (filteredData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No vacant berths found for class ${state.selectedClassCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredData) { detail ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(detail.from, 1.2f, isPrimary = true)
                                TableCell(detail.to, 1.2f, isPrimary = true)
                                TableCell(detail.coachName, 0.9f)
                                TableCell(detail.berthNumber.toString(), 1.0f, isBold = true)
                                TableCell(detail.berthCode, 0.8f)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(event: (HomeEvent) -> Unit, searchQuery: String) {
    val searchQueryState = remember { mutableStateOf(searchQuery) }

    OutlinedTextField(
        value = searchQueryState.value,
        onValueChange = {
            searchQueryState.value = it
            event(HomeEvent.updateSearchQuery(it))
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Filter by station or coach...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (searchQueryState.value.isNotEmpty()) {
                IconButton(onClick = {
                    searchQueryState.value = ""
                    event(HomeEvent.updateSearchQuery(""))
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
fun RowScope.TableHeader(
    text: String,
    weight: Float,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        ),
        modifier = Modifier
            .weight(weight)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center
    )
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isPrimary: Boolean = false,
    isBold: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        ),
        color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}