package com.vacart.di

import android.content.Context
import androidx.room.Room
import com.vacart.CustomJsonDeserializer
import com.vacart.api.TrainAPI
import com.vacart.client
import com.vacart.roomdatabase.AppDatabase
import com.vacart.roomdatabase.MIGRATION_1_2
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
    fun provideAppDatabase(@ApplicationContext app: Context): AppDatabase =
        Room.databaseBuilder(context = app, AppDatabase::class.java, "my_db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideSearchDao(db: AppDatabase) = db.searchDao()

    @Provides
    @Singleton
    fun provideVacartCacheDao(db: AppDatabase) = db.vacartCacheDao()

    @Provides
    @Singleton
    fun providePnrCacheDao(db: AppDatabase) = db.pnrCacheDao()

    @Provides
    @Singleton
    fun providePnrRecentSearchDao(db: AppDatabase) = db.pnrRecentSearchDao()
}