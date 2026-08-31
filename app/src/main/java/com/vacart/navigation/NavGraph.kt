package com.vacart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vacart.presentation.chat.Chat
import com.vacart.presentation.home.screens.Home
import com.vacart.presentation.home.HomeViewModel
import com.vacart.presentation.home.screens.BerthDetail
import com.vacart.presentation.home.screens.CoachDetail
import com.vacart.presentation.home.screens.VacancyChart
import com.vacart.presentation.pnr.PnrScreen

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
            composable(Routes.TrainSchedule.routes){
                val parentEntry = remember(it) {
                    navController.getBackStackEntry(Routes.HomeGraph.routes)
                }
                val homeViewMode: HomeViewModel = hiltViewModel(parentEntry)
                com.vacart.presentation.home.screens.TrainSchedule(navController, homeViewMode)
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
        navigation(
            startDestination = Routes.Pnr.routes,
            route = Routes.PnrGraph.routes
        ) {
            composable(Routes.Pnr.routes) {
                val pnrViewModel = hiltViewModel<com.vacart.presentation.pnr.PnrViewModel>()
                PnrScreen(viewModel = pnrViewModel)
            }
        }
        navigation(
            startDestination = Routes.TrainTracking.routes,
            route = Routes.TrainTrackingGraph.routes
        ) {
            composable(Routes.TrainTracking.routes) {
                val trackingViewModel = hiltViewModel<com.vacart.presentation.tracking.TrainTrackingViewModel>()
                com.vacart.presentation.tracking.TrainTrackingScreen(viewModel = trackingViewModel)
            }
        }

    }

}