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
    val observedAt: String = "",
    val temperature: Int = 18,
    val condition: String = "Clear Night",
    val feelsLike: Int = 18,
    val high: Int = 25,
    val low: Int = 14,
    val isDay: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasWeatherData: Boolean = false,
    val isOffline: Boolean = false,
    val isCachedData: Boolean = false,
    val lastSuccessfulUpdateEpochMillis: Long? = null,
    val provider: String = "Open-Meteo",
    val dataQualityLabel: String = "Unverified",
    val dataQualityWarnings: List<String> = emptyList(),
    val dataAgeMinutes: Long? = null,
    val validatedTimeZoneId: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val windSpeed: Int = 0,
    val windDirection: Int = 0,
    val windGusts: Int = 0,
    val humidity: Int = 0,
    val pressure: Int = 0,
    val visibilityKm: Double? = null,
    val dewPoint: Int = 0,
    val uvIndex: Double? = null,
    val cloudCover: Int = 0,
    val cloudCoverLow: Int = 0,
    val cloudCoverMid: Int = 0,
    val cloudCoverHigh: Int = 0,
    val directRadiation: Double = 0.0,
    val diffuseRadiation: Double = 0.0,
    val aqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
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
    ),
    val temperatureUnit: String = "°C",
    val windUnit: String = "km/h",
    val distanceUnit: String = "km",
    val languageCode: String = "en",
    val animationLevel: Int = 2
)
