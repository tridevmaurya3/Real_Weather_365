package com.tridev.realweather365.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.WeatherCacheStore
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.delay

data class SnowPhysics(
    val snowfallCm: Double,
    val precipitationChance: Int,
    val weatherCode: Int,
    val temperatureC: Int,
    val windSpeedKmh: Int,
    val windGustKmh: Int,
    val windDirection: Int,
    val visibilityKm: Double,
    val humidity: Int,
    val intensity: Float,
    val accumulation: Float,
    val blowingSnow: Float
)

@Composable
internal fun rememberSnowPhysics(): SnowPhysics {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { WeatherCacheStore(context) }
    val snapshot by produceState<LiveWeatherSnapshot?>(
        initialValue = store.loadLast()?.snapshot,
        key1 = store
    ) {
        while (true) {
            value = store.loadLast()?.snapshot
            delay(12_000L)
        }
    }
    return remember(snapshot) { calculateSnowPhysics(snapshot) }
}

private fun calculateSnowPhysics(snapshot: LiveWeatherSnapshot?): SnowPhysics {
    val currentHour = snapshot?.hourly24?.firstOrNull()
    val code = currentHour?.weatherCode ?: snapshot?.weatherCode ?: 73
    val snowfall = (currentHour?.snowfallCm ?: 0.0).coerceAtLeast(0.0)
    val chance = (currentHour?.rainChance ?: 0).coerceIn(0, 100)
    val temperature = snapshot?.temperature ?: -2
    val wind = (snapshot?.windSpeed ?: currentHour?.windSpeed ?: 0).coerceAtLeast(0)
    val gust = (snapshot?.windGusts ?: currentHour?.windGusts ?: wind).coerceAtLeast(wind)
    val visibility = (snapshot?.visibilityKm ?: 18.0).coerceIn(0.05, 80.0)
    val humidity = (snapshot?.humidity ?: 85).coerceIn(0, 100)

    val codeFloor = when (code) {
        71 -> 0.18f
        73 -> 0.42f
        75, 77 -> 0.78f
        85 -> 0.48f
        86 -> 0.82f
        else -> 0f
    }
    val measured = (snowfall / 1.35).coerceAtLeast(0.0).pow(0.55).toFloat().coerceIn(0f, 1f)
    val probabilityContribution = if (code in setOf(71, 73, 75, 77, 85, 86) || snowfall > 0.0) {
        chance / 100f * 0.28f
    } else {
        0f
    }
    val coldSupport = when {
        temperature <= -8 -> 0.10f
        temperature <= 0 -> 0.06f
        temperature <= 2 -> 0.02f
        else -> -0.10f
    }
    val intensity = (max(codeFloor, max(measured, probabilityContribution)) + coldSupport)
        .coerceIn(0.04f, 1f)

    val accumulation = (
        measured * 0.58f +
            intensity * 0.34f +
            if (temperature <= 0) 0.12f else 0f
        ).coerceIn(0f, 1f)

    val gustLift = ((gust - 24).coerceAtLeast(0) / 62f).coerceIn(0f, 1f)
    val windLift = ((wind - 18).coerceAtLeast(0) / 48f).coerceIn(0f, 1f)
    val blowingSnow = (intensity * 0.68f + gustLift * 0.55f + windLift * 0.25f)
        .coerceIn(0f, 1f)

    return SnowPhysics(
        snowfallCm = snowfall,
        precipitationChance = chance,
        weatherCode = code,
        temperatureC = temperature,
        windSpeedKmh = wind,
        windGustKmh = gust,
        windDirection = snapshot?.windDirection ?: 0,
        visibilityKm = visibility,
        humidity = humidity,
        intensity = intensity,
        accumulation = accumulation,
        blowingSnow = blowingSnow
    )
}
