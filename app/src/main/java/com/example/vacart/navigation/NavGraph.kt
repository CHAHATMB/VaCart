package com.example.vacart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.vacart.presentation.chat.Chat
import com.example.vacart.presentation.home.screens.Home
import com.example.vacart.presentation.home.HomeViewModel
import com.example.vacart.presentation.home.screens.BerthDetail
import com.example.vacart.presentation.home.screens.CoachDetail
import com.example.vacart.presentation.home.screens.VacancyChart

@Composable
fun NavGraph(navController: NavHostController){

    NavHost(navController = navController, startDestination = Routes.HomeGraph.routes ){
        navigation(
            startDestination = Routes.Home.routes,
            route = Routes.HomeGraph.routes
        ) {

            composable(Routes.Home.routes){
                val parentEntry = remember(it) {
                    navController.getBackStackEntry(Routes.HomeGraph.routes)
                }
                val homeViewMode: HomeViewModel = hiltViewModel(parentEntry)
                Home(navController, homeViewMode)
            }
            composable(Routes.VacancyChart.routes){
                val parentEntry = remember(it) {
                    navController.getBackStackEntry(Routes.HomeGraph.routes)
                }
                val homeViewMode: HomeViewModel = hiltViewModel(parentEntry)
                VacancyChart(navController, homeViewMode)
            }
            composable(Routes.BerthDetail.routes){
                val parentEntry = remember(it) {
                    navController.getBackStackEntry(Routes.HomeGraph.routes)
                }
                val homeViewMode: HomeViewModel = hiltViewModel(parentEntry)
                BerthDetail(navController, homeViewMode)
            }
            composable(Routes.CoachDetail.routes){
                val parentEntry = remember(it) {
                    navController.getBackStackEntry(Routes.HomeGraph.routes)
                }
                val homeViewMode: HomeViewModel = hiltViewModel(parentEntry)
                CoachDetail(navController, homeViewMode)
            }
        }
        navigation(
            startDestination = Routes.Chat.routes,
            route = Routes.ChatGraph.routes
        ) {
            composable(Routes.Chat.routes){
                Chat()
            }
        }

    }

}