package com.example.vacart.presentation.home.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vacart.model.TrainComposition
import com.example.vacart.navigation.Routes
import com.example.vacart.presentation.home.HomeEvent
import com.example.vacart.presentation.home.HomeState
import com.example.vacart.presentation.home.HomeViewModel


@Preview(showBackground = true)
@Composable
fun PreviewTrainDetailsScreen() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    VacancyChart(navController = navController, homeViewModel)
}

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) {
        Column (modifier = Modifier
            .padding(it)
            .padding(horizontal = 16.dp)) {
            // Train Details Section
            if( state.trainComposition == null ){
                Text(text = "Loading...")
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
            .background(Color.LightGray)
            .clip(shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(text = "Train Name: ${trainComposition?.trainName}", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Train Number: ${trainComposition?.trainNo}", style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Charting Station: ${trainComposition?.chartStatusResponseDto?.remoteStationCode}", style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Chart Created: ${trainComposition?.chartOneDate}", style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun VacantBirthSection(navController: NavController, state: HomeState, event: (HomeEvent)-> Unit) {
    Column {
        Text(text = "Vacant Birth", style = MaterialTheme.typography.h6)

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            val vacantBirths = state.trainComposition?.cdd?.flatMap { listOf(it.classCode) }?.distinct()
            vacantBirths?.forEach { classCode ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(Color.Gray, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp)
                        .clickable {
                            event(HomeEvent.selectClassCode(classCode))
                            navController.navigate(Routes.BerthDetail.routes)
                        }
                ) {
                    Text(text = classCode, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CoachStatusSection(navController: NavController, state: HomeState, event: (HomeEvent)-> Unit) {
    val coaches = state.trainComposition?.cdd?.flatMap { listOf(Pair(it.coachName, it.classCode) ) }?.distinct()?: emptyList()
    Column(modifier = Modifier.padding(bottom = 56.dp)) {
        Text(text = "Coach Status", style = MaterialTheme.typography.h6)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(coaches) { (coach, classcode) ->
                Text(
                    text = coach,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color.LightGray)
                        .padding(16.dp)
                        .clickable {
                            event(HomeEvent.selectClassCode(classcode))
                            event(HomeEvent.selectCoach(coach))
                            navController.navigate(Routes.CoachDetail.routes)
                        }
                )
            }
        }
    }
}