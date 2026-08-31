package com.vacart.navigation

sealed class Routes(val routes:String) {
    data object Home: Routes("home")
    data object HomeGraph: Routes("homegraph")
    data object ChatGraph: Routes("chatgraph")
    data object VacancyChart: Routes("vacancychart")
    data object Chat: Routes("chat")
    data object BerthDetail: Routes("berthdetail")
    data object CoachDetail: Routes("coachdetail")
    data object TrainSchedule: Routes("trainschedule")
    data object PnrGraph: Routes("pnrgraph")
    data object Pnr: Routes("pnr")
    data object TrainTrackingGraph: Routes("traintracking graph")
    data object TrainTracking: Routes("traintracking")

}