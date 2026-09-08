package com.tridev.realweather365.ui.home

data class WeatherHomeNavigation(
    val openLocations: () -> Unit = {},
    val openRadar: () -> Unit = {},
    val openForecast24: () -> Unit = {},
    val openForecast10: () -> Unit = {},
    val openAirQuality: () -> Unit = {},
    val openAlerts: () -> Unit = {},
    val openDetails: () -> Unit = {}
)
