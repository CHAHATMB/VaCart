package com.example.vacart.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.vacart.CustomJsonDeserializer
import com.example.vacart.api.TrainAPI
import com.example.vacart.client
import com.example.vacart.roomdatabase.AppDatabase
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext app: Context): AppDatabase = Room.databaseBuilder(context = app,
        AppDatabase::class.java, "my_db").build()

    @Provides
    @Singleton
    fun provideYourDao(db: AppDatabase) = db.searchDao()

}