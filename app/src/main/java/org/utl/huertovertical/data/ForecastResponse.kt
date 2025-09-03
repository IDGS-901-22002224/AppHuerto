package org.utl.huertovertical.data

import com.google.gson.annotations.SerializedName

// Respuesta general del pronostico de 5 días
data class ForecastResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: City
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val sys: ForecastSys,
    val dt_txt: String,
    val rain: Rain? = null,
    val snow: Snow? = null
)

data class Rain(
    @SerializedName("1h") val oneHour: Double? = null
)

data class Snow(
    @SerializedName("1h") val oneHour: Double? = null
)

data class ForecastSys(
    val pod: String
)

data class City(
    val id: Long,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)