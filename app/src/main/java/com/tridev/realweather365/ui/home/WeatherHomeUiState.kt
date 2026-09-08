package com.tridev.realweather365.ui.home

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
    val updatedAt: String = "Live • 10:48 PM",
    val temperature: Int = 18,
    val condition: String = "Clear Night",
    val feelsLike: Int = 18,
    val high: Int = 25,
    val low: Int = 14,
    val hourly: List<HourForecast> = listOf(
        HourForecast("Now", 18, "Clear Night"),
        HourForecast("11 PM", 17, "Clear Night"),
        HourForecast("1 AM", 16, "Clear Night"),
        HourForecast("3 AM", 15, "Clear Night"),
        HourForecast("5 AM", 14, "Clear Night"),
        HourForecast("7 AM", 15, "Cloudy")
    ),
    val metrics: List<WeatherMetric> = listOf(
        WeatherMetric("AQI", "54", "Moderate"),
        WeatherMetric("Wind", "10 km/h", "NE"),
        WeatherMetric("Humidity", "63%", "Comfortable"),
        WeatherMetric("Pressure", "1014", "hPa")
    )
)
