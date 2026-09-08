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
import com.tridev.realweather365.ui.forecast.Forecast24HourScreen
import com.tridev.realweather365.ui.home.WeatherHomeScreen
import com.tridev.realweather365.ui.home.WeatherHomeViewModel
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
                var destination by rememberSaveable { mutableStateOf("forecast24") }

                BackHandler(enabled = destination != "weather") {
                    destination = "weather"
                }

                when (destination) {
                    "radar" -> RadarScreen(
                        location = uiState.value.location,
                        onBack = { destination = "weather" }
                    )

                    "forecast24" -> Forecast24HourScreen(
                        location = uiState.value.location,
                        onBack = { destination = "weather" }
                    )

                    else -> WeatherHomeScreen(uiState.value)
                }
            }
        }
    }
}
