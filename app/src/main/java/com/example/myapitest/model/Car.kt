package com.example.myapitest.model

data class Place(
    val lat: Double,
    val long: Double
)

data class Car(
    val id: String,
    val value: CarDetails,
)

data class CarDetails(
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: Place
)
