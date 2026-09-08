package com.tridev.realweather365

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tridev.realweather365.ui.airquality.AirQualityScreen
import com.tridev.realweather365.ui.alerts.SevereWeatherAlertScreen
import com.tridev.realweather365.ui.details.WeatherDetailsScreen
import com.tridev.realweather365.ui.forecast.Forecast10DayScreen
import com.tridev.realweather365.ui.forecast.Forecast24HourScreen
import com.tridev.realweather365.ui.home.WeatherHomeNavigation
import com.tridev.realweather365.ui.home.WeatherHomeScreen
import com.tridev.realweather365.ui.home.WeatherHomeViewModel
import com.tridev.realweather365.ui.location.GlobalLocationScreen
import com.tridev.realweather365.ui.radar.RadarScreen
import com.tridev.realweather365.ui.theme.RealWeather365Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            RealWeather365Theme {
                val viewModel: WeatherHomeViewModel = viewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                var destination by rememberSaveable { mutableStateOf("weather") }

                BackHandler(enabled = destination != "weather") {
                    destination = "weather"
                }

                when (destination) {
                    "radar" -> RadarScreen(
                        location = uiState.value.location,
                        onBack = { destination = "weather" }
                    )

                    "forecast24" -> Forecast24HourScreen(
                        state = uiState.value,
                        onBack = { destination = "weather" }
                    )

                    "forecast10" -> Forecast10DayScreen(
                        state = uiState.value,
                        onBack = { destination = "weather" }
                    )

                    "airQuality" -> AirQualityScreen(
                        state = uiState.value,
                        onBack = { destination = "weather" }
                    )

                    "severeAlert" -> SevereWeatherAlertScreen(
                        location = uiState.value.location,
                        onBack = { destination = "weather" }
                    )

                    "weatherDetails" -> WeatherDetailsScreen(
                        state = uiState.value,
                        onBack = { destination = "weather" }
                    )

                    "globalLocation" -> GlobalLocationScreen(
                        selectedLocation = uiState.value.selectedLocation,
                        onLocationSelected = { location ->
                            viewModel.selectLocation(location)
                            destination = "weather"
                        },
                        onBack = { destination = "weather" }
                    )

                    else -> WeatherHomeScreen(
                        state = uiState.value,
                        navigation = WeatherHomeNavigation(
                            openLocations = { destination = "globalLocation" },
                            openRadar = { destination = "radar" },
                            openForecast24 = { destination = "forecast24" },
                            openForecast10 = { destination = "forecast10" },
                            openAirQuality = { destination = "airQuality" },
                            openAlerts = { destination = "severeAlert" },
                            openDetails = { destination = "weatherDetails" }
                        )
                    )
                }
            }
        }
    }
}
