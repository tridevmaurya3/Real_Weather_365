package com.tridev.realweather365.ui.home

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.location.WorldLocationCatalog
import com.tridev.realweather365.data.weather.CachedWeatherRecord
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.OpenMeteoWeatherRepository
import com.tridev.realweather365.data.weather.WeatherCacheStore
import com.tridev.realweather365.data.weather.WeatherDataAccuracyEngine
import com.tridev.realweather365.data.weather.WeatherDataRejectedException
import com.tridev.realweather365.data.weather.WmoWeather
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val weatherRepository = OpenMeteoWeatherRepository()
    private val accuracyEngine = WeatherDataAccuracyEngine()
    private val cacheStore = WeatherCacheStore(application)
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _uiState = MutableStateFlow(WeatherHomeUiState())
    val uiState: StateFlow<WeatherHomeUiState> = _uiState.asStateFlow()

    private var desiredLocation: WorldLocation = WorldLocationCatalog.chandauli
    private var requestGeneration: Long = 0L

    @Volatile
    private var networkAvailable: Boolean = isNetworkAvailableNow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkState(isNetworkAvailableNow())
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val available = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            updateNetworkState(available)
        }

        override fun onLost(network: Network) {
            updateNetworkState(isNetworkAvailableNow())
        }
    }

    init {
        cacheStore.loadLast()?.let { cached ->
            desiredLocation = cached.location
            _uiState.value = stateFromSnapshot(
                location = cached.location,
                snapshot = cached.snapshot,
                updatedAt = cachedStatus(cached, offline = !networkAvailable, refreshing = networkAvailable),
                isLoading = false,
                isOffline = !networkAvailable,
                isCachedData = true,
                savedAtEpochMillis = cached.savedAtEpochMillis,
                errorMessage = null
            )
        }

        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
        refreshWeather(desiredLocation)
    }

    fun selectLocation(location: WorldLocation) {
        desiredLocation = location
        val cached = cacheStore.loadFor(location.id)

        if (cached != null) {
            _uiState.value = stateFromSnapshot(
                location = cached.location,
                snapshot = cached.snapshot,
                updatedAt = cachedStatus(cached, offline = !networkAvailable, refreshing = networkAvailable),
                isLoading = false,
                isOffline = !networkAvailable,
                isCachedData = true,
                savedAtEpochMillis = cached.savedAtEpochMillis,
                errorMessage = null
            )
        } else if (networkAvailable) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isOffline = false,
                updatedAt = "Loading ${location.name}…"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isOffline = true,
                errorMessage = "No saved weather for ${location.name}",
                updatedAt = "Offline • ${location.name} has no saved weather"
            )
            return
        }

        refreshWeather(location)
    }

    fun refreshWeather(location: WorldLocation = desiredLocation) {
        desiredLocation = location
        val requestId = ++requestGeneration

        if (!isNetworkAvailableNow()) {
            networkAvailable = false
            showOfflineFallback(location, requestId, "No internet connection")
            return
        }

        networkAvailable = true
        if (!_uiState.value.hasWeatherData) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isOffline = false,
                errorMessage = null,
                updatedAt = "Updating live weather…"
            )
        }

        viewModelScope.launch {
            val result = loadWithRetry(location)
            if (requestId != requestGeneration) return@launch

            result.onSuccess { snapshot ->
                val savedAt = System.currentTimeMillis()
                cacheStore.save(location, snapshot, savedAt)
                _uiState.value = stateFromSnapshot(
                    location = location,
                    snapshot = snapshot,
                    updatedAt = "Live • ${snapshot.displayTime}",
                    isLoading = false,
                    isOffline = false,
                    isCachedData = false,
                    savedAtEpochMillis = savedAt,
                    errorMessage = null
                )
            }.onFailure { error ->
                showOfflineFallback(
                    location = location,
                    requestId = requestId,
                    reason = error.message ?: "Unable to load live weather"
                )
            }
        }
    }

    private suspend fun loadWithRetry(location: WorldLocation): Result<LiveWeatherSnapshot> {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val raw = weatherRepository.load(location)
                val accuracy = accuracyEngine.validateLive(location, raw)
                if (accuracy.isRejected) {
                    throw WeatherDataRejectedException(
                        accuracy.warnings.firstOrNull() ?: "Weather response failed accuracy validation"
                    )
                }
                return Result.success(accuracy.snapshot)
            } catch (error: Throwable) {
                lastError = error
                if (attempt < 2 && isNetworkAvailableNow()) {
                    delay(700L * (attempt + 1))
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Weather update failed"))
    }

    private fun showOfflineFallback(location: WorldLocation, requestId: Long, reason: String) {
        if (requestId != requestGeneration) return
        val cached = cacheStore.loadFor(location.id)
        if (cached != null) {
            _uiState.value = stateFromSnapshot(
                location = cached.location,
                snapshot = cached.snapshot,
                updatedAt = cachedStatus(cached, offline = true, refreshing = false),
                isLoading = false,
                isOffline = true,
                isCachedData = true,
                savedAtEpochMillis = cached.savedAtEpochMillis,
                errorMessage = "$reason • showing saved weather"
            )
            return
        }

        val current = _uiState.value
        _uiState.value = if (current.hasWeatherData) {
            current.copy(
                isLoading = false,
                isOffline = true,
                errorMessage = "$reason • no saved weather for ${location.name}",
                updatedAt = "Offline • couldn't update ${location.name} • showing ${current.location}"
            )
        } else {
            current.copy(
                location = location.name,
                selectedLocation = location,
                condition = "Weather unavailable",
                isLoading = false,
                isOffline = true,
                isCachedData = false,
                errorMessage = reason,
                updatedAt = "Offline • no saved weather yet"
            )
        }
    }

    private fun updateNetworkState(available: Boolean) {
        val changed = available != networkAvailable
        networkAvailable = available
        if (!changed) return

        viewModelScope.launch {
            if (available) {
                _uiState.value = _uiState.value.copy(
                    updatedAt = if (_uiState.value.hasWeatherData) "Back online • refreshing…" else "Internet restored • loading weather…",
                    isOffline = false,
                    errorMessage = null
                )
                refreshWeather(desiredLocation)
            } else if (_uiState.value.hasWeatherData) {
                val savedAt = _uiState.value.lastSuccessfulUpdateEpochMillis ?: System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isOffline = true,
                    isCachedData = true,
                    isLoading = false,
                    updatedAt = "Offline • last weather ${ageLabel(savedAt)}"
                )
            }
        }
    }

    private fun stateFromSnapshot(
        location: WorldLocation,
        snapshot: LiveWeatherSnapshot,
        updatedAt: String,
        isLoading: Boolean,
        isOffline: Boolean,
        isCachedData: Boolean,
        savedAtEpochMillis: Long,
        errorMessage: String?
    ): WeatherHomeUiState {
        val accuracy = accuracyEngine.validateCached(location, snapshot)
        val safeSnapshot = accuracy.snapshot
        val condition = WmoWeather.condition(safeSnapshot.weatherCode, safeSnapshot.isDay)
        val hourly = safeSnapshot.hourly24.take(6).map { hour ->
            HourForecast(
                time = hour.label,
                temperature = hour.temperature,
                condition = WmoWeather.condition(hour.weatherCode, hour.isDay)
            )
        }

        return _uiState.value.copy(
            scene = sceneFor(safeSnapshot.weatherCode, safeSnapshot.isDay, safeSnapshot.observedAt),
            location = location.name,
            selectedLocation = location,
            updatedAt = "$updatedAt • ${accuracy.label}",
            observedAt = safeSnapshot.observedAt,
            temperature = safeSnapshot.temperature,
            condition = condition,
            feelsLike = safeSnapshot.feelsLike,
            high = safeSnapshot.high,
            low = safeSnapshot.low,
            isDay = safeSnapshot.isDay,
            isLoading = isLoading,
            errorMessage = errorMessage,
            hasWeatherData = true,
            isOffline = isOffline,
            isCachedData = isCachedData,
            lastSuccessfulUpdateEpochMillis = savedAtEpochMillis,
            dataQualityLabel = accuracy.label,
            dataQualityWarnings = accuracy.warnings,
            dataAgeMinutes = accuracy.dataAgeMinutes,
            validatedTimeZoneId = accuracy.timeZoneId,
            sunrise = safeSnapshot.sunrise,
            sunset = safeSnapshot.sunset,
            windSpeed = safeSnapshot.windSpeed,
            windDirection = safeSnapshot.windDirection,
            windGusts = safeSnapshot.windGusts,
            humidity = safeSnapshot.humidity,
            pressure = safeSnapshot.pressure,
            visibilityKm = safeSnapshot.visibilityKm,
            dewPoint = safeSnapshot.dewPoint,
            uvIndex = safeSnapshot.uvIndex,
            cloudCover = safeSnapshot.cloudCover,
            cloudCoverLow = safeSnapshot.cloudCoverLow,
            cloudCoverMid = safeSnapshot.cloudCoverMid,
            cloudCoverHigh = safeSnapshot.cloudCoverHigh,
            directRadiation = safeSnapshot.directRadiation,
            diffuseRadiation = safeSnapshot.diffuseRadiation,
            aqi = safeSnapshot.aqi,
            pm25 = safeSnapshot.pm25,
            pm10 = safeSnapshot.pm10,
            nitrogenDioxide = safeSnapshot.nitrogenDioxide,
            ozone = safeSnapshot.ozone,
            hourly24 = safeSnapshot.hourly24,
            daily10 = safeSnapshot.daily10,
            hourly = if (hourly.isNotEmpty()) hourly else _uiState.value.hourly,
            metrics = listOf(
                WeatherMetric("AQI", safeSnapshot.aqi?.toString() ?: "--", WmoWeather.aqiLabel(safeSnapshot.aqi)),
                WeatherMetric("Wind", "${safeSnapshot.windSpeed} km/h", WmoWeather.compassDirection(safeSnapshot.windDirection)),
                WeatherMetric("Humidity", "${safeSnapshot.humidity}%", humidityHint(safeSnapshot.humidity)),
                WeatherMetric("Pressure", safeSnapshot.pressure.toString(), "hPa")
            )
        )
    }

    private fun cachedStatus(record: CachedWeatherRecord, offline: Boolean, refreshing: Boolean): String {
        val age = ageLabel(record.savedAtEpochMillis)
        return when {
            offline -> "Offline cache • saved $age"
            refreshing -> "Saved weather • refreshing… • $age"
            else -> "Saved weather • $age"
        }
    }

    private fun ageLabel(savedAtEpochMillis: Long): String {
        if (savedAtEpochMillis <= 0L) return "earlier"
        val minutes = ((System.currentTimeMillis() - savedAtEpochMillis).coerceAtLeast(0L) / 60_000L)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1_440 -> "${minutes / 60} h ago"
            else -> "${minutes / 1_440} d ago"
        }
    }

    private fun isNetworkAvailableNow(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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

    override fun onCleared() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
    }
}
