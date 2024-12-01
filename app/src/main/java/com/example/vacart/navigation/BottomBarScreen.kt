package com.example.vacart.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarScreen(
    val route: String,
    val title : String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen(route = "home", title = "Home", icon = Icons.Default.Home)
    object Chat : BottomBarScreen(route = "chat", title = "Chat", icon = Icons.Default.Menu)
}