package com.vacart.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Train
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarScreen(
    val route: String,
    val title : String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen(route = "home", title = "Home", icon = Icons.Default.Home)
    object Pnr : BottomBarScreen(route = "pnr", title = "PNR", icon = Icons.Default.ConfirmationNumber)
    object Chat : BottomBarScreen(route = "chat", title = "Chat", icon = Icons.Default.Menu)
    object TrainTracker : BottomBarScreen(route = "traintracking", title = "Tracker", icon = Icons.Default.Train)
}