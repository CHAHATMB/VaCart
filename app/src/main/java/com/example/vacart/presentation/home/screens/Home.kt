package com.example.vacart.presentation.home.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vacart.util.getDateBasedOnOffset
import androidx.navigation.NavController
import com.example.vacart.navigation.Routes
import com.example.vacart.presentation.home.HomeEvent
import com.example.vacart.presentation.home.HomeState
import com.example.vacart.presentation.home.HomeViewModel
import kotlin.reflect.KFunction1

@Composable
fun Home(navController: NavController, homeViewModel: HomeViewModel){
    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()

    val dateList = arrayOf("2 days ago", "Yesterday", "Today", "Tomorrow")
    var selectedText by remember { mutableStateOf("") }

    Scaffold (
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VaCart",
                    fontSize = 24.sp
                )
            }
        }
    ){
        Column(modifier = Modifier
            .padding(it)
            .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = "Journey Detail",
                fontSize = 18.sp
            )

            TextField(
                value = state.selectedTrain,
                onValueChange = {
                    selectedText = it
                    state.selectedTrain = it
                    event(HomeEvent.updateTrainNumber(it))
                },
                label = { Text(text = "Train Number") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            dropDownMenu(state =state, dateList = dateList) { sT ->
//                selectedDate = sT
                state.journeyDate = getDateBasedOnOffset(sT - 2)
                println("selected date" + state.journeyDate);
                event(HomeEvent.updateSelectDate(dateList[sT]))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { event(HomeEvent.selectTrain(state.trainNumber))
                navController.navigate(Routes.VacancyChart.routes)}) {
                Text(text = "Get Chart")
            }
        }

    }

}


@Composable
fun dropDownMenu(
    state: HomeState,
    dateList: Array<String>,
    onChange: (Int)->Unit
) {

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var textfieldSize by remember { mutableStateOf(Size.Zero) }


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
                onClick = { expanded = !expanded
                    Log.d("VaChart", "logning - ${expanded}")
                }
            ) {
                if(!expanded){
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = ""
                    )

                } else{
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = ""
                    )
                }

            }},
    )

            DropdownMenu(
                modifier = Modifier
                    .width(200.dp)
                    .wrapContentSize(),
                expanded = expanded,
                onDismissRequest = {
                    // We shouldn't hide the menu when the user enters/removes any character
                },
                offset = DpOffset(50.dp,0.dp)
            ) {
                dateList.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            selectedText = item
                            expanded = false
                            Log.d("VaChart","$item selected")
                            onChange(index)
                        }
                    )
                }
            }



}