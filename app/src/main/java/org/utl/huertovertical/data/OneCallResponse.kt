package org.utl.huertovertical.data

import com.google.gson.annotations.SerializedName

data class OneCallResponse(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    @SerializedName("timezone_offset") val timezoneOffset: Int,
    val current: CurrentWeather,
    val daily: List<DailyForecast>,

)

data class CurrentWeather(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val uvi: Double,
    val clouds: Int,
    val visibility: Int,
    val wind_speed: Double,
    val wind_deg: Int,
    val wind_gust: Double?,
    val weather: List<WeatherDescription>,
    val rain: Double?,
    val snow: Double?
)

data class DailyForecast(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val moonrise: Long,
    val moonset: Long,
    val moon_phase: Double,
    val temp: DailyTemp,
    @SerializedName("feels_like") val feelsLike: DailyFeelsLike,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val wind_speed: Double,
    val wind_deg: Int,
    val wind_gust: Double?,
    val weather: List<WeatherDescription>,
    val clouds: Int,
    val pop: Double,
    val uvi: Double,
    // ¡CORREGIDO! rain y snow ahora son directamente Double?
    val rain: Double?, // Volumen de lluvia (puede ser null)
    val snow: Double? // Volumen de nieve (puede ser null)
)

data class DailyTemp(
    val day: Double,
    val min: Double,
    val max: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

data class DailyFeelsLike(
    val day: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

data class WeatherDescription(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

// ¡IMPORTANTE! Las clases RainCurrent y SnowCurrent HAN SIDO ELIMINADAS de este archivo
// porque ya no se usan ni se corresponden con la estructura de datos que esperamos para
// la API 2.5 de pronóstico de 5 días.