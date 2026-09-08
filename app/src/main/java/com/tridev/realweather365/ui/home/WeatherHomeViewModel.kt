package com.tridev.realweather365.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.weather.OpenMeteoWeatherRepository
import com.tridev.realweather365.data.weather.WmoWeather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherHomeViewModel(
    private val weatherRepository: OpenMeteoWeatherRepository = OpenMeteoWeatherRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherHomeUiState())
    val uiState: StateFlow<WeatherHomeUiState> = _uiState.asStateFlow()

    init {
        refreshWeather(_uiState.value.selectedLocation)
    }

    fun selectLocation(location: WorldLocation) {
        _uiState.value = _uiState.value.copy(
            location = location.name,
            selectedLocation = location,
            updatedAt = "Updating live weather…",
            isLoading = true,
            errorMessage = null
        )
        refreshWeather(location)
    }

    fun refreshWeather(location: WorldLocation = _uiState.value.selectedLocation) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            updatedAt = "Updating live weather…",
            errorMessage = null
        )

        viewModelScope.launch {
            runCatching { weatherRepository.load(location) }
                .onSuccess { snapshot ->
                    if (_uiState.value.selectedLocation.id != location.id) return@onSuccess

                    val condition = WmoWeather.condition(snapshot.weatherCode, snapshot.isDay)
                    val hourly = snapshot.hourly24.take(6).map { hour ->
                        HourForecast(
                            time = hour.label,
                            temperature = hour.temperature,
                            condition = WmoWeather.condition(hour.weatherCode, hour.isDay)
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        scene = sceneFor(
                            weatherCode = snapshot.weatherCode,
                            isDay = snapshot.isDay,
                            observedAt = snapshot.observedAt
                        ),
                        location = location.name,
                        selectedLocation = location,
                        updatedAt = "Live • ${snapshot.displayTime}",
                        temperature = snapshot.temperature,
                        condition = condition,
                        feelsLike = snapshot.feelsLike,
                        high = snapshot.high,
                        low = snapshot.low,
                        isLoading = false,
                        errorMessage = null,
                        sunrise = snapshot.sunrise,
                        sunset = snapshot.sunset,
                        hourly24 = snapshot.hourly24,
                        daily10 = snapshot.daily10,
                        hourly = if (hourly.isNotEmpty()) hourly else _uiState.value.hourly,
                        metrics = listOf(
                            WeatherMetric(
                                "AQI",
                                snapshot.aqi?.toString() ?: "--",
                                WmoWeather.aqiLabel(snapshot.aqi)
                            ),
                            WeatherMetric(
                                "Wind",
                                "${snapshot.windSpeed} km/h",
                                WmoWeather.compassDirection(snapshot.windDirection)
                            ),
                            WeatherMetric(
                                "Humidity",
                                "${snapshot.humidity}%",
                                humidityHint(snapshot.humidity)
                            ),
                            WeatherMetric(
                                "Pressure",
                                snapshot.pressure.toString(),
                                "hPa"
                            )
                        )
                    )
                }
                .onFailure { error ->
                    if (_uiState.value.selectedLocation.id != location.id) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        updatedAt = "Weather unavailable • tap location to retry",
                        errorMessage = error.message ?: "Unable to load live weather"
                    )
                }
        }
    }

    private fun sceneFor(weatherCode: Int, isDay: Boolean, observedAt: String): WeatherScene {
        return when (weatherCode) {
            71, 73, 75, 77, 85, 86 -> WeatherScene.SNOW
            95, 96, 99 -> WeatherScene.THUNDERSTORM
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherScene.RAIN
            else -> {
                if (!isDay) {
                    WeatherScene.NIGHT
                } else {
                    val hour = observedAt.substringAfter('T', "12:00").take(2).toIntOrNull() ?: 12
                    if (hour in 5..7) WeatherScene.SUNRISE else WeatherScene.SUNNY
                }
            }
        }
    }

    private fun humidityHint(humidity: Int): String = when {
        humidity < 35 -> "Dry"
        humidity <= 70 -> "Comfortable"
        else -> "Humid"
    }
}
