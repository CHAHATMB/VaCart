package com.example.vacart.presentation.home.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vacart.model.Cdd
import com.example.vacart.model.ChartStatusResponseDto
import com.example.vacart.model.TrainComposition
import com.example.vacart.navigation.Routes
import com.example.vacart.presentation.home.HomeEvent
import com.example.vacart.presentation.home.HomeState
import com.example.vacart.presentation.home.HomeViewModel
import com.example.vacart.ui.theme.VaCartTheme


@Preview(showBackground = true)
@Composable
fun PreviewTrainDetailsScreen() {
//    VaCartTheme {
        Column {
//            TrainDetailBox(
//                TrainComposition(
//                    "",
//                    listOf<Cdd>(),
//                    "",
//                    ChartStatusResponseDto(1, 1, 0, "", "", ""),
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    ""
//                )
//            )
            val navController = rememberNavController()
            val cdd = listOf<Cdd>(Cdd("A1", "B1", 0, 12), Cdd("B1", "B1", 1, 12))
            val trainComposition = TrainComposition(cdd)
            VacantBirthSection(navController, state = HomeState(trainComposition), event = {})
            CoachStatusSection(navController, state = HomeState(trainComposition), event = {})
            LoadingScreen()
        }
    }
//}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacancyChart(navController: NavController, homeViewModel: HomeViewModel){

    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()

    LaunchedEffect(state.trainNumber) {
        event(HomeEvent.getTrainComposition(state.trainNumber))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.background(Color.Blue),
                title = { Text("Train Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) {
        Column (modifier = Modifier
            .padding(it)
            .padding(horizontal = 16.dp)) {
            // Train Details Section
            if( state.isLoading){
                LoadingScreen()
            } else if(state.showError){
                ErrorPage(navController = navController, errorMessage = "Something went wrong! Please try again later.")
            } else {
                TrainDetailBox(state.trainComposition!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Vacant Birth Section
                VacantBirthSection(navController, state, event)

                Spacer(modifier = Modifier.height(16.dp))

                // Coach Status Section
                CoachStatusSection(navController, state, event)
            }
        }
    }
}

@Composable
fun TrainDetailBox(trainComposition: TrainComposition) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Column {
            Text(text = "Train Name: ${trainComposition?.trainName}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Train Number: ${trainComposition?.trainNo}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Charting Station: ${trainComposition?.chartStatusResponseDto?.remoteStationCode}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Chart Created: ${trainComposition?.chartOneDate}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun VacantBirthSection(navController: NavController, state: HomeState, event: (HomeEvent)-> Unit) {
    Column {
        Text(text = "Vacant Birth", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            val vacantBirths = state.trainComposition?.cdd?.flatMap { listOf(it.classCode) }?.distinct()
            vacantBirths?.forEach { classCode ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(8.dp)
                        .clickable {
                            event(HomeEvent.selectClassCode(classCode))
                            navController.navigate(Routes.BerthDetail.routes)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = classCode, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center )
                }
            }
        }
    }
}

@Composable
fun CoachStatusSection(navController: NavController, state: HomeState, event: (HomeEvent)-> Unit) {
    val coaches = state.trainComposition?.cdd?.flatMap { listOf(it) }?.distinct()?: emptyList()
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = "Coach Status", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 94.dp)
        ){
            items(coaches.sortedBy { coach -> coach.classCode }) {
                Column(modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        event(HomeEvent.selectClassCode(it.classCode))
                        event(HomeEvent.selectCoach(it.coachName))
                        navController.navigate(Routes.CoachDetail.routes)
                    }
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${it.coachName} | ${it.classCode}",
                        modifier = Modifier
                            .fillMaxWidth(),

                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Avl: ${it.vacantBerths} ",
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We are fetching train data, please wait...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ErrorPage(navController: NavController, errorMessage: String) {
    // Column to layout error message and actions
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Error Message Text
            Text(
                text = "Oops!",
                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = errorMessage,
                style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Go Back", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}