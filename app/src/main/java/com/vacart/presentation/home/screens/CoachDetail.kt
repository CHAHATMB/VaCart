package com.vacart.presentation.home.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vacart.R
import com.vacart.model.Bdd
import com.vacart.model.Bsd
import com.vacart.presentation.home.HomeEvent
import com.vacart.presentation.home.HomeState
import com.vacart.presentation.home.HomeViewModel
import com.vacart.presentation.home.util.getSeatName
import com.vacart.presentation.home.util.is1A
import com.vacart.presentation.home.util.is2A
import com.vacart.presentation.home.util.leftLine
import com.vacart.presentation.home.util.rightLine
import com.vacart.util.getBirthOccupancyColor
import kotlin.math.ceil

val LocalViewModel = compositionLocalOf<HomeViewModel> { error("No ViewModel provided") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachDetail(navController: NavController, homeViewModel: HomeViewModel = hiltViewModel()) {
    val event = homeViewModel::onEvent
    val state by homeViewModel.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedCoach) {
        event(HomeEvent.getCoachComposition())
    }
    CompositionLocalProvider(LocalViewModel provides homeViewModel) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Coach Composition",
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize()
            ) {
                if (state.trainComposition == null) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = "Loading coach visualizer...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Coach: ${state.coachComposition?.coachName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Occupancy Info",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
    val divider = if (state.selectedClassCode.is1A()) 4 else if (state.selectedClassCode.is2A()) 6 else 8
    val items = (1..totalCoaches + (ceil(totalCoaches.toDouble() / divider).toInt() * 2)).map { it.toString() }
    Column {
        val fixedCell = if (state.selectedClassCode.is1A()) 3 else if (state.selectedClassCode.is2A()) 4 else 5
        LazyVerticalGrid(
            columns = GridCells.Fixed(fixedCell),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            if (state.selectedClassCode.is2A()) {
                itemsIndexed(items) { index, _ ->
                    var counter = 2 * maxOf(0, ((index + 1) - 4) / 8 + 1)
                    if (index < 4) counter = 0
                    var actualIndex = (index + 1) - counter
                    if ((index + 1) % 8 == 4) {
                        actualIndex += 3
                    }
                    if (index == 3) actualIndex = 5
                    if (index in (0..3)) {
                        HorizontalLine()
                    }

                    if (index % 4 != 2 && actualIndex <= totalCoaches) {
                        val colorState = getBirthOccupancyColor(data?.get(actualIndex - 1), state.stationList)
                        Column(
                            modifier = Modifier.then(
                                if (index % 4 == 0)
                                    Modifier.leftLine()
                                else if (index % 4 == 3)
                                    Modifier.rightLine()
                                else Modifier
                            )
                        ) {
                            if (index % 8 in (4..7)) {
                                NumberIcon(actualIndex, 90f, colorState) {
                                    selectedBirth = it - 1
                                    showSheet = true
                                }
                                HorizontalLine()
                            } else {
                                NumberIcon(actualIndex, colorState = colorState) {
                                    selectedBirth = it - 1
                                    showSheet = true
                                }
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(items) { index, _ ->
                    var counter = 2 * maxOf(0, ((index + 1) - 6) / 10 + 1)
                    if (index < 5) counter = 0
                    var actualIndex = (index + 1) - counter
                    if ((index + 1) % 10 == 5) {
                        actualIndex += 2
                    }
                    if (index in (0..4)) {
                        HorizontalLine()
                    }
                    val colorState = getBirthOccupancyColor(data?.get(actualIndex - 1), state.stationList)

                    if (index % 5 != 3) {
                        Column(
                            modifier = Modifier.then(
                                if (index % 5 == 0)
                                    Modifier.leftLine()
                                else if (index % 5 == 4)
                                    Modifier.rightLine()
                                else Modifier
                            )
                        ) {
                            if (index % 10 in (5..9)) {
                                NumberIcon(actualIndex, 90f, colorState) {
                                    selectedBirth = it - 1
                                    showSheet = true
                                }
                                HorizontalLine()
                            } else {
                                NumberIcon(actualIndex, colorState = colorState) {
                                    selectedBirth = it - 1
                                    showSheet = true
                                }
                            }
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
            .background(MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp)
    )
}

@Composable
fun NumberIcon(
    number: Int,
    rotationAngle: Float = 270f,
    colorState: Int = (1..3).random(),
    classCode: String = "3A",
    onClick: (Int) -> Unit = {}
) {
    val alignment = if (rotationAngle == 270f) Alignment.TopCenter else Alignment.BottomCenter
    val padding = if (rotationAngle == 270f) PaddingValues(top = 12.dp) else PaddingValues(bottom = 12.dp)
    val birthPosition = getSeatName(number, classCode = classCode)
    val color = when (colorState) {
        1 -> Color(0xFF80D4DC).copy(alpha = 0.85f)
        2 -> Color(0xFFFFF59D).copy(alpha = 0.85f)
        3 -> Color(0xFFA5D89D).copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(6.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .clickable { onClick(number) }
    ) {
        Icon(
            painter = painterResource(R.drawable.seat_image),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(rotationZ = rotationAngle)
                .background(color),
            tint = Color.Black
        )
        Column(
            modifier = Modifier
                .align(alignment)
                .padding(padding)
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = number.toString(),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = birthPosition,
                color = Color.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(data: Bdd? = null, onDismiss: () -> Unit) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    val viewModel = LocalViewModel.current
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        if (data != null) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Berth Number: ${data.berthNo}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalLine()
                Spacer(modifier = Modifier.height(16.dp))
                if (data.cabinCoupe != null)
                    Text(
                        text = "Coupe: ${data.cabinCoupe}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                if (data.cabinCoupeNameNo != null)
                    Text(
                        text = "Cabin No: ${data.cabinCoupeNameNo}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                Text(
                    text = "Occupancy Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)) {
                    data.bsd?.map {
                        val occ = if (it.occupancy) "Occupied" else "Vacant"
                        Text(
                            text = "${it.splitNo}. ${viewModel.getStationName(it.from)} (${it.from}) -> ${viewModel.getStationName(it.to)} (${it.to})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            buildAnnotatedString {
                                append("Status: ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(occ)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                        Text(
                            text = "Quota: ${it.quota}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = "Occupancy Legend",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalLine()
            val indicatorData = listOf(
                indicator(Color(0xFF80D4DC), "Occupied for full journey"),
                indicator(Color(0xFFFFF59D), "Occupied for part journey"),
                indicator(Color(0xFFA5D89D), "Vacant for full journey"),
            )
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                indicatorData.forEach {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(it.color, shape = RoundedCornerShape(6.dp))
                                .border(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = it.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

data class indicator(val color: Color, val description: String)