package com.example.vacart.di

import com.example.vacart.CustomJsonDeserializer
import com.example.vacart.api.TrainAPI
import com.example.vacart.client
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
//@InstallIn(ViewModelComponent::class)
object AppModule {

    var gson1 = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(String::class.java, CustomJsonDeserializer())
        .create()

    @Provides
    fun provideBaseUrl(): String = "https://www.irctc.co.in"

    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson1))
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): TrainAPI = retrofit.create(TrainAPI::class.java)

}