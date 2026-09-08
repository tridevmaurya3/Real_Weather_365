package com.tridev.realweather365.ui.home

enum class WeatherScene {
    SUNNY,
    SUNRISE,
    RAIN,
    THUNDERSTORM
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
    val scene: WeatherScene = WeatherScene.THUNDERSTORM,
    val location: String = "Chandauli",
    val updatedAt: String = "Live • 8:32 PM",
    val temperature: Int = 26,
    val condition: String = "Thunderstorm",
    val feelsLike: Int = 29,
    val high: Int = 30,
    val low: Int = 23,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 26, "Thunderstorm"),
        HourForecast("9 PM", 25, "Thunderstorm"),
        HourForecast("11 PM", 24, "Thunderstorm"),
        HourForecast("1 AM", 24, "Rain"),
        HourForecast("3 AM", 23, "Rain"),
        HourForecast("5 AM", 23, "Cloudy")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "71", "Moderate"),
        WeatherMetric("Wind", "22 km/h", "SSE"),
        WeatherMetric("Humidity", "87%", "Very humid"),
        WeatherMetric("Pressure", "1004", "hPa")
    )
)
