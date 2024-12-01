package com.example.vacart.presentation.home.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.vacart.navigation.Routes
import com.example.vacart.presentation.home.HomeViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.vacart.model.Vbd
import com.example.vacart.presentation.home.HomeEvent

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
            TopAppBar(
                title = { Text("Vacant Berth Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {
            // Search Bar
           SearchBar(event = event, searchQuery = searchQuery)

            Spacer(modifier = Modifier.height(16.dp))

            // Table header
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)) {
                TableHeader("From Station",1f) {event(HomeEvent.sortByColumn("fromStation"))}
                TableHeader("To Station",1f) {event(HomeEvent.sortByColumn("toStation"))}
                TableHeader("Coach",1f) {event(HomeEvent.sortByColumn("coach"))}
                TableHeader("Berth No",1f)
                TableHeader("Berth Type",1f)
            }
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)){
                // Filtered Data
                val filteredData = berthDetails.filter {
                    it.from.contains(searchQuery, ignoreCase = true) ||
                            it.to.contains(searchQuery, ignoreCase = true) ||
                            it.coachName.contains(searchQuery, ignoreCase = true)
                }
                items(filteredData){detail ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center) {
                        TableCell(detail.from,1f)
                        TableCell(detail.to,1f)
                        TableCell(detail.coachName,1f)
                        TableCell(detail.berthNumber.toString(),1f)
                        TableCell(detail.berthCode,1f)
                    }
                }

            }
            Spacer(modifier = Modifier.height(128.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTrainDetailsScrdeen() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
   SearchBar(event = {}, searchQuery = "")
}



@Composable
fun SearchBar(event: (HomeEvent) -> Unit, searchQuery: String) {
    val searchQueryState = remember { mutableStateOf(searchQuery) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 12.dp)
            .height(56.dp), // Set height to standard search bar height
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQueryState.value,
                onValueChange = {
                    searchQueryState.value = it
                    event(HomeEvent.updateSearchQuery(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide()}),
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                decorationBox = { innerTextField ->
                    if (searchQueryState.value.isEmpty()) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    innerTextField()
                },

            )



        }
    }
}

@Composable
fun RowScope.TableHeader(
    text: String,
    weight: Float,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .height(50.dp)
            .padding(8.dp)
            .clickable(onClick != null) { onClick?.invoke() },
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
}


@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float
) {
    Text(
        text = text,
        Modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .padding(8.dp),
        textAlign = TextAlign.Center
    )
}