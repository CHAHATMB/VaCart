package com.example.vacart.presentation.home.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vacart.R
import com.example.vacart.api.TrainAPI
import com.example.vacart.di.AppModule
import com.example.vacart.model.Bdd
import com.example.vacart.model.Bsd
import com.example.vacart.model.Cdd
import com.example.vacart.presentation.home.HomeEvent
import com.example.vacart.presentation.home.HomeState
import com.example.vacart.presentation.home.HomeViewModel
import com.example.vacart.presentation.home.util.leftLine
import com.example.vacart.presentation.home.util.rightLine
import com.example.vacart.util.getBirthOccupancyColor
import kotlin.random.Random

val LocalViewModel = compositionLocalOf<HomeViewModel> { error("No ViewModel provided") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachDetail(navController: NavController, homeViewModel: HomeViewModel = hiltViewModel()) {
    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedCoach) {
        println("making api call")
        event(HomeEvent.getCoachComposition())
    }
    CompositionLocalProvider(LocalViewModel provides homeViewModel){
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.background(Color.Blue),
                    title = { Text("Coach Composition") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            }
        ) {
            Column (modifier = Modifier
                .padding(it)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
                verticalArrangement = Arrangement.Center) {
                // Train Details Section
                if( state.trainComposition == null ){
                    Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "Loading...")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically){
                        Text(text = "Coach : ${state.coachComposition?.coachName}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                showSheet = true
                            }, contentAlignment = Alignment.CenterEnd){
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = "Info")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    MyLazyVerticalGrid(state)
                }
            }
        }
        if (showSheet) {
            BottomSheet(data = null) {
                showSheet = false
            }
        }
    }

}



@Composable
fun MyLazyVerticalGrid(state: HomeState) {
    var showSheet by remember { mutableStateOf(false) }
    var selectedBirth by remember { mutableStateOf(0) }
    val totalCoaches = state.coachComposition?.bdd?.size ?: 0
    val data = state.coachComposition?.bdd
    val items = (1..totalCoaches + (totalCoaches/8*2)).map{it.toString()} // Sample data
    Column {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                var counter = (2 * maxOf(0, ((index+1)-6)/10+1))
                if(index<5) counter = 0
                var actualIndex = (index+1) - counter
                if((index+1)%10 == 5){
                    actualIndex += 2
                }
                if(index in (0..4)){
                    HorizontalLine()
                }
                val colorState = getBirthOccupancyColor(data?.get(actualIndex-1), state.stationList)

                if(index%5 != 3){
                    Column(modifier = Modifier
                        .then(
                            if (index % 5 == 0)
                                Modifier.leftLine()
                            else if (index % 5 == 4)
                                Modifier.rightLine()
                            else Modifier

                        )
//                        .clickable {
//                            selectedBirth = actualIndex
//                            showSheet = true
//                            println("clicking!! $selectedBirth")
//                        }
                    ) {

                        // Add a horizontal line after every two items
                        if (index % 10 in (5..9)) {
                            NumberIcon(actualIndex,90f, colorState, onClick = {selectedBirth = it-1
                                showSheet = true
                                println("clicking!! $selectedBirth")})
                            HorizontalLine()
                        } else {
                            NumberIcon(actualIndex, colorState = colorState){selectedBirth = it-1
                                showSheet = true
                                println("clicking!! $selectedBirth")}
                        }
                    }
                }

            }
        }
    }
    if (showSheet) {
        BottomSheet(data = state.coachComposition?.bdd?.get(selectedBirth)) {
            showSheet = false
        }
    }
}


@Composable
fun HorizontalLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Gray)
            .padding(8.dp)
    )
}


@Composable
fun NumberIcon(number: Int, rotationAngle: Float = 270f, colorState: Int = (1..3).random(), onClick : (Int)->Unit = {}) {
    val alignment =
        if(rotationAngle == 270f ){ Alignment.TopCenter} else {Alignment.BottomCenter}
    val padding =
        if(rotationAngle == 270f ){ PaddingValues(top = 12.dp)
        } else {PaddingValues(bottom = 12.dp)}
    val birthPosition = when(number%8){
        1, 4 -> "L"
        2, 5 -> "M"
        3, 6 -> "U"
        7 -> "SL"
        0 -> "SU"
        else -> ""
    }
    val color = when(colorState){
        1 -> Color(0xFF9effff)
        2 -> Color(0xFFfffd9e)
        3 -> Color(0xFFa5ff9e)
        else -> Color(0xFFf6f7d7)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .padding(8.dp)
        .clip(shape = RoundedCornerShape(8.dp))
        .clickable { onClick(number) }) {
        Icon(
            painter = painterResource(R.drawable.seat_image),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp) // Adjust the size as needed
                .graphicsLayer(rotationZ = rotationAngle)
                .background(color),
            tint = Color.Black
        )
        Column(modifier = Modifier
            .align(alignment)
            .padding(padding)) {

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = number.toString(),
                color = Color.Black, // Text color
                style = MaterialTheme.typography.bodyMedium // Text style
            )
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = birthPosition,
                color = Color.Black, // Text color
                style = MaterialTheme.typography.titleSmall,
//                modifier = Modifier.size(16.dp)
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun PRe() {
    BottomSheet(
        data = Bdd(
            berthCode = "AB",
            berthNo = 32,
            bsd = listOf(Bsd(from = "A", occupancy = false, quota = "as", splitNo = 0, to = "B")),
            cabinCoupe = "",
            cabinCoupeNameNo = "",
            enable = true,
            from= "String",
            quotaCntStn= "Any",
            to= "String"
        ),
        onDismiss = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(data : Bdd? = null, onDismiss: () -> Unit) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    val viewModel = LocalViewModel.current
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        if(data != null){
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Text(text="Berth Number: ${data?.berthNo}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalLine()
                Spacer(modifier = Modifier.height(16.dp))
                if(data.cabinCoupe!=null)
                    Text(text = "Coupe: ${data.cabinCoupe}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                if(data.cabinCoupeNameNo != null)
                    Text(text = "Cabin No: ${data.cabinCoupeNameNo}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                Text(text="Occupancy Status: ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold)
                Column(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                    data.bsd?.map {
                        val occ = if(it.occupancy) "Occupied" else "Vacant"
                        Text( text = "${it.splitNo}. ${viewModel.getStationName(it.from)} (${it.from}) -> ${viewModel.getStationName(it.to)} (${it.to})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            buildAnnotatedString {
                                append("Occupancy: ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(occ)
                                }
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )
                        Text(text = "Quota: ${it.quota}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text="Occupancy Indicator",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalLine()
            val indicatorData = listOf(
                indicator(Color(0xFF9effff), "Occupied for full journey"),
                indicator(Color(0xFFfffd9e), "Occupied for part journey"),
                indicator(Color(0xFFa5ff9e), "Vacant for full journey"),
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                indicatorData  .forEach{
                    Row(modifier = Modifier.padding(vertical=8.dp), verticalAlignment = Alignment.CenterVertically){
                        Box(modifier = Modifier
                            .size(36.dp)
                            .background(it.color)
                            .padding(4.dp)
                            .clip(shape = RoundedCornerShape(4.dp))
                            .border(border = BorderStroke(1.dp, Color.Black))
                        )
                        Text(text=" -> ${it.description}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                }
            }
        }

    }
}

data class indicator(val color: Color, val description: String)