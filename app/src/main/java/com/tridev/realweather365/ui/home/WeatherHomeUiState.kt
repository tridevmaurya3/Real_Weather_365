package com.tridev.realweather365.ui.home

enum class WeatherScene {
    SUNNY,
    SUNRISE,
    RAIN
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
    val scene: WeatherScene = WeatherScene.RAIN,
    val location: String = "Chandauli",
    val updatedAt: String = "Live • 4:36 PM",
    val temperature: Int = 27,
    val condition: String = "Rain",
    val feelsLike: Int = 30,
    val high: Int = 29,
    val low: Int = 24,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 27, "Rain"),
        HourForecast("5 PM", 27, "Rain"),
        HourForecast("7 PM", 26, "Rain"),
        HourForecast("9 PM", 26, "Rain"),
        HourForecast("11 PM", 25, "Cloudy"),
        HourForecast("1 AM", 24, "Cloudy")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "68", "Moderate"),
        WeatherMetric("Wind", "18 km/h", "SE"),
        WeatherMetric("Humidity", "94%", "Very humid"),
        WeatherMetric("Pressure", "1007", "hPa")
    )
)
