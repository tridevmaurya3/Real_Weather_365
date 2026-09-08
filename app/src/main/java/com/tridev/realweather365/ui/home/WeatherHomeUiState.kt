package com.tridev.realweather365.ui.home

enum class WeatherScene {
    SUNNY,
    SUNRISE
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
    val scene: WeatherScene = WeatherScene.SUNRISE,
    val location: String = "Chandauli",
    val updatedAt: String = "Live • 6:12 AM",
    val temperature: Int = 20,
    val condition: String = "Sunrise",
    val feelsLike: Int = 21,
    val high: Int = 27,
    val low: Int = 15,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 20, "Sunrise"),
        HourForecast("7 AM", 21, "Sunny"),
        HourForecast("9 AM", 22, "Sunny"),
        HourForecast("11 AM", 25, "Sunny"),
        HourForecast("1 PM", 27, "Sunny"),
        HourForecast("3 PM", 26, "Sunny")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "58", "Moderate"),
        WeatherMetric("Wind", "8 km/h", "E"),
        WeatherMetric("Humidity", "72%", "Humid"),
        WeatherMetric("Pressure", "1009", "hPa")
    )
)
