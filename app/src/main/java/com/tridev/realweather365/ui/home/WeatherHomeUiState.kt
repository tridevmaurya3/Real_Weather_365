package com.tridev.realweather365.ui.home

import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.location.WorldLocationCatalog
import com.tridev.realweather365.data.weather.LiveDayData
import com.tridev.realweather365.data.weather.LiveHourData

enum class WeatherScene {
    SUNNY,
    SUNRISE,
    RAIN,
    THUNDERSTORM,
    SNOW,
    NIGHT
}

data class HourForecast(
    val time: String,
    val temperature: Int,
    val condition: String = "Sunny"
)

data class WeatherMetric(
    val label: String,
    val value: String,
    val hint: String
)

data class WeatherHomeUiState(
    val scene: WeatherScene = WeatherScene.NIGHT,
    val location: String = "Chandauli",
    val selectedLocation: WorldLocation = WorldLocationCatalog.chandauli,
    val updatedAt: String = "Connecting to live weather…",
    val temperature: Int = 18,
    val condition: String = "Clear Night",
    val feelsLike: Int = 18,
    val high: Int = 25,
    val low: Int = 14,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val provider: String = "Open-Meteo",
    val sunrise: String = "",
    val sunset: String = "",
    val hourly24: List<LiveHourData> = emptyList(),
    val daily10: List<LiveDayData> = emptyList(),
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 18, "Clear Night"),
        HourForecast("11 PM", 17, "Clear Night"),
        HourForecast("1 AM", 16, "Clear Night"),
        HourForecast("3 AM", 15, "Clear Night"),
        HourForecast("5 AM", 14, "Clear Night"),
        HourForecast("7 AM", 15, "Cloudy")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "--", "Loading"),
        WeatherMetric("Wind", "-- km/h", "--"),
        WeatherMetric("Humidity", "--%", "Loading"),
        WeatherMetric("Pressure", "----", "hPa")
    )
)
