package com.example.myapitest.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService = instance.create(ApiService::class.java)

    suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.Success(apiCall())
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    Result.Error(e.code(), e.message())
                }
                else -> {
                    Result.Error(-1, "Unknown error")
                }
            }
        }
    }

    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val code: Int, val message: String) : Result<Nothing>()
    }
}
