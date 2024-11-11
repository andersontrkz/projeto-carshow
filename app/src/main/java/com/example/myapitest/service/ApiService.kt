package com.example.myapitest.service

import com.example.myapitest.model.Car
import com.example.myapitest.model.CarDetails
import retrofit2.http.*

interface ApiService {
    @GET("car")
    suspend fun getCars(): List<CarDetails>

    @GET("car/{id}")
    suspend fun getCar(@Path("id") id: String): Car

    @POST("car")
    suspend fun addCar(@Body car: CarDetails): CarDetails

    @POST("car")
    suspend fun addCars(@Body cars: List<CarDetails>): List<CarDetails>

    @PATCH("car/{id}")
    suspend fun updateCar(@Path("id") id: String, @Body car: CarDetails): CarDetails

    @DELETE("car/{id}")
    suspend fun deleteCar(@Path("id") id: String)
}
