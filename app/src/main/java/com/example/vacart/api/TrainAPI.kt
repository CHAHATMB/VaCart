package com.example.vacart.api

import com.example.vacart.model.CoachComposition
import com.example.vacart.model.CoachCompositionRequest
import com.example.vacart.model.StationList
import com.example.vacart.model.TrainComposition
import com.example.vacart.model.TrainInfoRequest
import com.example.vacart.model.VacantBerth
import com.example.vacart.model.VacantBerthRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface TrainAPI {
    
    @Headers("greq: 1705426938144")
    @GET("/eticketing/protected/mapps1/trnscheduleenquiry/{trainNumber}")
    suspend fun getStationList(@Path("trainNumber") trainNumber: String): Response<StationList>

    @POST("/online-charts/api/trainComposition")
    suspend fun getTrainComposition(@Body trainInfoRequest: TrainInfoRequest): Response<TrainComposition>

    @POST("/online-charts/api/vacantBerth")
    suspend fun getVacantBerth(@Body vacantBerthRequest: VacantBerthRequest): Response<VacantBerth>

    @POST("/online-charts/api/coachComposition")
    suspend fun getCoachComposition(@Body coachCompositionRequest: CoachCompositionRequest): Response<CoachComposition>

}