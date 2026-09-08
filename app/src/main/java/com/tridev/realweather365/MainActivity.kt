package com.tridev.realweather365

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tridev.realweather365.data.preferences.BackgroundWorld
import com.tridev.realweather365.data.preferences.WeatherPreferences
import com.tridev.realweather365.data.preferences.WeatherPreferencesStore
import com.tridev.realweather365.notification.WeatherNotificationScheduler
import com.tridev.realweather365.ui.airquality.AirQualityScreen
import com.tridev.realweather365.ui.alerts.SevereWeatherAlertScreen
import com.tridev.realweather365.ui.details.WeatherDetailsScreen
import com.tridev.realweather365.ui.forecast.Forecast10DayScreen
import com.tridev.realweather365.ui.forecast.Forecast24HourScreen
import com.tridev.realweather365.ui.home.WeatherHomeNavigation
import com.tridev.realweather365.ui.home.WeatherHomeScreen
import com.tridev.realweather365.ui.home.WeatherHomeViewModel
import com.tridev.realweather365.ui.home.WeatherScene
import com.tridev.realweather365.ui.location.GlobalLocationScreen
import com.tridev.realweather365.ui.personalization.AnimatedBackgroundsScreen
import com.tridev.realweather365.ui.personalization.PersonalizationHubScreen
import com.tridev.realweather365.ui.personalization.RealSmartNotificationsScreen
import com.tridev.realweather365.ui.personalization.RealWidgetsScreen
import com.tridev.realweather365.ui.personalization.SettingsScreen
import com.tridev.realweather365.ui.radar.RadarScreen
import com.tridev.realweather365.ui.theme.RealWeather365Theme
import com.tridev.realweather365.widget.WeatherWidgetUpdater

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
                val preferencesStore = remember { WeatherPreferencesStore(this@MainActivity) }
                var preferences by remember { mutableStateOf(preferencesStore.load()) }
                var destination by rememberSaveable { mutableStateOf("weather") }
                var notificationPermissionGranted by remember {
                    mutableStateOf(hasNotificationRuntimePermission())
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    notificationPermissionGranted = granted || hasNotificationRuntimePermission()
                }

                val displayedHomeState = if (preferences.automaticBackground) {
                    uiState.value
                } else {
                    uiState.value.copy(scene = preferences.manualBackground.toWeatherScene())
                }

                val updatePreferences: (WeatherPreferences) -> Unit = { updated ->
                    preferences = updated
                    preferencesStore.save(updated)
                }

                LaunchedEffect(preferences) {
                    WeatherNotificationScheduler.apply(this@MainActivity, preferences)
                }

                LaunchedEffect(uiState.value) {
                    if (!uiState.value.isLoading) {
                        WeatherWidgetUpdater.syncFromApp(this@MainActivity, uiState.value)
                    }
                }

                BackHandler(enabled = destination != "weather") {
                    destination = when (destination) {
                        "widgets", "animatedBackgrounds", "smartNotifications", "settings" -> "personalization"
                        else -> "weather"
                    }
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

                    "personalization" -> PersonalizationHubScreen(
                        state = displayedHomeState,
                        preferences = preferences,
                        onBack = { destination = "weather" },
                        onOpenWidgets = { destination = "widgets" },
                        onOpenBackgrounds = { destination = "animatedBackgrounds" },
                        onOpenNotifications = {
                            notificationPermissionGranted = hasNotificationRuntimePermission()
                            destination = "smartNotifications"
                        },
                        onOpenSettings = { destination = "settings" }
                    )

                    "widgets" -> RealWidgetsScreen(
                        state = displayedHomeState,
                        onBack = { destination = "personalization" }
                    )

                    "animatedBackgrounds" -> AnimatedBackgroundsScreen(
                        preferences = preferences,
                        onPreferencesChanged = updatePreferences,
                        onBack = { destination = "personalization" }
                    )

                    "smartNotifications" -> RealSmartNotificationsScreen(
                        preferences = preferences,
                        permissionGranted = notificationPermissionGranted,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationPermissionGranted = true
                            }
                        },
                        onPreferencesChanged = updatePreferences,
                        onBack = { destination = "personalization" }
                    )

                    "settings" -> SettingsScreen(
                        preferences = preferences,
                        onPreferencesChanged = updatePreferences,
                        onBack = { destination = "personalization" }
                    )

                    else -> WeatherHomeScreen(
                        state = displayedHomeState,
                        navigation = WeatherHomeNavigation(
                            openLocations = { destination = "globalLocation" },
                            openRadar = { destination = "radar" },
                            openForecast24 = { destination = "forecast24" },
                            openForecast10 = { destination = "forecast10" },
                            openAirQuality = { destination = "airQuality" },
                            openAlerts = { destination = "severeAlert" },
                            openDetails = { destination = "weatherDetails" },
                            openPersonalization = { destination = "personalization" }
                        )
                    )
                }
            }
        }
    }

    private fun hasNotificationRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun BackgroundWorld.toWeatherScene(): WeatherScene = when (this) {
    BackgroundWorld.CLEAR_SKY -> WeatherScene.SUNNY
    BackgroundWorld.SUNRISE -> WeatherScene.SUNRISE
    BackgroundWorld.RAIN -> WeatherScene.RAIN
    BackgroundWorld.STORM -> WeatherScene.THUNDERSTORM
    BackgroundWorld.SNOW -> WeatherScene.SNOW
    BackgroundWorld.NIGHT -> WeatherScene.NIGHT
}
