package com.tridev.realweather365.ui.home

enum class WeatherScene {
    SUNNY,
    SUNRISE,
    RAIN,
    THUNDERSTORM,
    SNOW
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
    val scene: WeatherScene = WeatherScene.SNOW,
    val location: String = "Chandauli",
    val updatedAt: String = "Live • 9:17 AM",
    val temperature: Int = -2,
    val condition: String = "Snow",
    val feelsLike: Int = -5,
    val high: Int = 2,
    val low: Int = -8,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", -2, "Snow"),
        HourForecast("11 AM", -1, "Snow"),
        HourForecast("1 PM", 0, "Snow"),
        HourForecast("3 PM", 1, "Snow"),
        HourForecast("5 PM", 0, "Cloudy"),
        HourForecast("7 PM", -2, "Snow")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "54", "Moderate"),
        WeatherMetric("Wind", "14 km/h", "NW"),
        WeatherMetric("Humidity", "78%", "Humid"),
        WeatherMetric("Pressure", "1012", "hPa")
    )
)
