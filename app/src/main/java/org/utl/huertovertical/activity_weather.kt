package org.utl.huertovertical

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.utl.huertovertical.api.RetrofitClient
import org.utl.huertovertical.adapter.ForecastAdapter
import org.utl.huertovertical.data.DailyForecast
import org.utl.huertovertical.data.ForecastItem
import org.utl.huertovertical.data.ForecastResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class activity_weather : AppCompatActivity() {

    private lateinit var tvLocation: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvWeatherIcon: ImageView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvWeatherDescription: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvUvIndex: TextView
    private lateinit var tvRainChance: TextView

    private lateinit var rvForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter
    private val forecastList = mutableListOf<DailyForecast>()

    private val API_KEY = "ae2b9a1bd2e8599fd834ab32b944b9cb"
    private val LATITUDE = 21.1254
    private val LONGITUDE = -101.6708

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("onCreate ejecutado, llamando a fetchWeatherData")
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvLocation = findViewById(R.id.tvLocation)
        tvDate = findViewById(R.id.tvDate)
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon)
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp)
        tvWeatherDescription = findViewById(R.id.tvWeatherDescription)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvUvIndex = findViewById(R.id.tvUvIndex)
        tvRainChance = findViewById(R.id.tvRainChance)

        rvForecast = findViewById(R.id.rvForecast)
        rvForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        forecastAdapter = ForecastAdapter(forecastList)
        rvForecast.adapter = forecastAdapter

        fetchWeatherData()
    }

    private fun fetchWeatherData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("Iniciando solicitud a OpenWeatherMap...")
                val response = RetrofitClient.weatherApiService.getFiveDayForecast(
                    lat = LATITUDE,
                    lon = LONGITUDE,
                    apiKey = API_KEY,
                    units = "metric",
                    lang = "es"
                )
                println("Respuesta recibida - Código: ${response.code()}, Mensaje: ${response.message()}")

                if (response.isSuccessful && response.body() != null) {
                    val forecastData = response.body()!!
                    withContext(Dispatchers.Main) {
                        if (forecastData.list.isNotEmpty()) {
                            val currentForecastItem = forecastData.list.first()
                            updateCurrentWeatherUI(currentForecastItem, forecastData.city.name)
                        } else {
                            Toast.makeText(this@activity_weather, "No se encontraron datos de pronóstico.", Toast.LENGTH_LONG).show()
                        }

                        val dailyForecastsProcessed = mutableListOf<DailyForecast>()
                        val seenDates = mutableSetOf<String>()
                        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                        for (item in forecastData.list) {
                            val dateString = dayFormat.format(Date(item.dt * 1000L))
                            if (dateString !in seenDates) {
                                seenDates.add(dateString)
                                dailyForecastsProcessed.add(
                                    DailyForecast(
                                        dt = item.dt,
                                        sunrise = 0,
                                        sunset = 0,
                                        moonrise = 0,
                                        moonset = 0,
                                        moon_phase = 0.0,
                                        temp = org.utl.huertovertical.data.DailyTemp(
                                            day = item.main.temp,
                                            min = item.main.temp_min,
                                            max = item.main.temp_max,
                                            night = item.main.temp,
                                            eve = item.main.temp,
                                            morn = item.main.temp
                                        ),
                                        feelsLike = org.utl.huertovertical.data.DailyFeelsLike(
                                            day = item.main.feels_like,
                                            night = item.main.feels_like,
                                            eve = item.main.feels_like,
                                            morn = item.main.feels_like
                                        ),
                                        pressure = item.main.pressure,
                                        humidity = item.main.humidity,
                                        dew_point = 0.0,
                                        wind_speed = item.wind.speed,
                                        wind_deg = item.wind.deg,
                                        wind_gust = item.wind.gust,
                                        weather = listOf(
                                            org.utl.huertovertical.data.WeatherDescription(
                                                id = item.weather.firstOrNull()?.id ?: 0,
                                                main = item.weather.firstOrNull()?.main ?: "",
                                                description = item.weather.firstOrNull()?.description ?: "",
                                                icon = item.weather.firstOrNull()?.icon ?: ""
                                            )
                                        ),
                                        clouds = item.clouds.all,
                                        pop = item.pop,
                                        uvi = 0.0,
                                        rain = item.rain?.oneHour,
                                        snow = item.snow?.oneHour
                                    )
                                )
                            }
                        }
                        forecastList.clear()
                        forecastList.addAll(dailyForecastsProcessed.take(5))
                        forecastAdapter.notifyDataSetChanged()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@activity_weather, "Error al cargar datos del clima: ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                println("Excepción capturada: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@activity_weather, "Error de red o API: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCurrentWeatherUI(currentForecastItem: ForecastItem, cityName: String) {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("es", "MX"))
        val currentDate = dateFormat.format(Date(currentForecastItem.dt * 1000L))

        tvLocation.text = cityName
        tvDate.text = currentDate
        tvCurrentTemp.text = "${currentForecastItem.main.temp.toInt()}°C"
        tvWeatherDescription.text = currentForecastItem.weather.firstOrNull()?.description?.capitalize(Locale("es", "MX")) ?: "N/A"

        tvHumidity.text = "Humedad: ${currentForecastItem.main.humidity}%"
        tvWindSpeed.text = "Viento: ${String.format("%.1f", currentForecastItem.wind.speed)} m/s"
        tvUvIndex.text = "UV: N/A" // UV Index is not available in Forecast API 2.5
        tvRainChance.text = "Prob. Lluvia Hoy: ${(currentForecastItem.pop * 100).toInt()}%"

        val iconName = currentForecastItem.weather.firstOrNull()?.icon
        if (iconName != null) {
            val iconResourceId = resources.getIdentifier("icon_$iconName", "drawable", packageName)
            if (iconResourceId != 0) {
                tvWeatherIcon.setImageResource(iconResourceId)
            } else {
                tvWeatherIcon.setImageResource(R.drawable.ic_weather_placeholder)
            }
        } else {
            tvWeatherIcon.setImageResource(R.drawable.ic_weather_placeholder)
        }
    }
}