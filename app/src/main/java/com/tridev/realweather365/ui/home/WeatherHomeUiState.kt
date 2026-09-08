package com.tridev.realweather365.ui.home

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
    val location: String = "Chandauli",
    val updatedAt: String = "Live • Updated now",
    val temperature: Int = 24,
    val condition: String = "Sunny",
    val feelsLike: Int = 26,
    val high: Int = 27,
    val low: Int = 15,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 24),
        HourForecast("9 AM", 25),
        HourForecast("11 AM", 26),
        HourForecast("1 PM", 27),
        HourForecast("3 PM", 28),
        HourForecast("5 PM", 27)
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "42", "Good"),
        WeatherMetric("Wind", "12 km/h", "NE"),
        WeatherMetric("Humidity", "48%", "Comfortable"),
        WeatherMetric("Pressure", "1008", "hPa")
    )
)
