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

data class PrecipitationPhysics(
    val precipitationMm: Double,
    val rainChance: Int,
    val weatherCode: Int,
    val cape: Double,
    val windSpeedKmh: Int,
    val windGustKmh: Int,
    val windDirection: Int,
    val intensity: Float,
    val lightningPotential: Float,
    val thunderstorm: Boolean,
    val hail: Boolean
)

@Composable
internal fun rememberPrecipitationPhysics(storm: Boolean): PrecipitationPhysics {
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
    return remember(snapshot, storm) { calculatePrecipitationPhysics(snapshot, storm) }
}

private fun calculatePrecipitationPhysics(
    snapshot: LiveWeatherSnapshot?,
    forceStorm: Boolean
): PrecipitationPhysics {
    val currentHour = snapshot?.hourly24?.firstOrNull()
    val code = currentHour?.weatherCode ?: snapshot?.weatherCode ?: if (forceStorm) 95 else 63
    val precipitation = (currentHour?.precipitationMm ?: 0.0).coerceAtLeast(0.0)
    val rainChance = (currentHour?.rainChance ?: 0).coerceIn(0, 100)
    val cape = (currentHour?.cape ?: 0.0).coerceAtLeast(0.0)
    val wind = (snapshot?.windSpeed ?: currentHour?.windSpeed ?: 0).coerceAtLeast(0)
    val gust = (snapshot?.windGusts ?: currentHour?.windGusts ?: wind).coerceAtLeast(wind)
    val windDirection = snapshot?.windDirection ?: 0

    val codeFloor = when (code) {
        51, 53, 55, 56, 57 -> 0.16f
        61 -> 0.25f
        63 -> 0.43f
        65, 66, 67 -> 0.72f
        80 -> 0.38f
        81 -> 0.58f
        82 -> 0.90f
        95 -> 0.68f
        96, 99 -> 0.88f
        else -> 0f
    }
    val measured = (precipitation / 7.0).coerceAtLeast(0.0).pow(0.55).toFloat().coerceIn(0f, 1f)
    val probabilityContribution = (rainChance / 100f * 0.34f).coerceIn(0f, 0.34f)
    val stormFloor = if (forceStorm) 0.55f else 0f
    val intensity = max(max(codeFloor, measured), max(probabilityContribution, stormFloor)).coerceIn(0f, 1f)

    val thunder = forceStorm || code in setOf(95, 96, 99)
    val hail = code == 96 || code == 99
    val codeLightning = when (code) {
        95 -> 0.46f
        96 -> 0.68f
        99 -> 0.84f
        else -> if (forceStorm) 0.34f else 0f
    }
    val capeContribution = (cape / 2200.0).toFloat().coerceIn(0f, 0.72f)
    val gustContribution = ((gust - 35).coerceAtLeast(0) / 70f * 0.22f).coerceIn(0f, 0.22f)
    val rainContribution = intensity * 0.18f
    val lightning = if (thunder) {
        max(codeLightning, capeContribution + gustContribution + rainContribution).coerceIn(0.18f, 1f)
    } else {
        0f
    }

    return PrecipitationPhysics(
        precipitationMm = precipitation,
        rainChance = rainChance,
        weatherCode = code,
        cape = cape,
        windSpeedKmh = wind,
        windGustKmh = gust,
        windDirection = windDirection,
        intensity = intensity,
        lightningPotential = lightning,
        thunderstorm = thunder,
        hail = hail
    )
}
