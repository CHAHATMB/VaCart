package com.example.vacart.navigation

sealed class Routes(val routes:String) {
    data object Home: Routes("home")
    data object HomeGraph: Routes("homegraph")
    data object ChatGraph: Routes("chatgraph")
    data object VacancyChart: Routes("vacancychart")
    data object Chat: Routes("chat")
    data object BerthDetail: Routes("berthdetail")
    data object CoachDetail: Routes("coachdetail")

}