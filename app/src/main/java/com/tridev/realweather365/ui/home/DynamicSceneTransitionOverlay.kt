package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.WeatherCacheStore
import kotlin.math.max
import kotlinx.coroutines.delay

data class SceneTransitionTargets(
    val night: Float,
    val twilight: Float,
    val cloud: Float,
    val rain: Float,
    val storm: Float,
    val snow: Float,
    val fog: Float
)

/**
 * Stage 36 continuous atmosphere blender.
 * Weather worlds still keep their dedicated renderers, while this layer interpolates the visual
 * exposure and atmosphere between provider refreshes so a world change never feels like a hard cut.
 */
@Composable
fun DynamicSceneTransitionOverlay(
    animationLevel: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { WeatherCacheStore(context) }
    val snapshot by produceState<LiveWeatherSnapshot?>(
        initialValue = store.loadLast()?.snapshot,
        key1 = store
    ) {
        while (true) {
            value = store.loadLast()?.snapshot
            delay(10_000L)
        }
    }

    val targets = remember(snapshot) { sceneTargets(snapshot) }
    val duration = when {
        animationLevel <= 0 -> 0
        animationLevel == 1 -> 1_450
        else -> 2_800
    }
    val spec = tween<Float>(durationMillis = duration)

    val night by animateFloatAsState(targets.night, spec, label = "scene-night-blend")
    val twilight by animateFloatAsState(targets.twilight, spec, label = "scene-twilight-blend")
    val cloud by animateFloatAsState(targets.cloud, spec, label = "scene-cloud-blend")
    val rain by animateFloatAsState(targets.rain, spec, label = "scene-rain-blend")
    val storm by animateFloatAsState(targets.storm, spec, label = "scene-storm-blend")
    val snow by animateFloatAsState(targets.snow, spec, label = "scene-snow-blend")
    val fog by animateFloatAsState(targets.fog, spec, label = "scene-fog-blend")

    if (night + twilight + cloud + rain + storm + snow + fog < 0.025f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        if (night > 0.015f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF001126).copy(alpha = 0.22f * night),
                    0.48f to Color(0xFF031A2B).copy(alpha = 0.12f * night),
                    1f to Color(0xFF020A10).copy(alpha = 0.08f * night)
                )
            )
        }

        if (twilight > 0.015f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF372B5A).copy(alpha = 0.08f * twilight),
                    0.43f to Color(0xFFFF8C5C).copy(alpha = 0.12f * twilight),
                    0.66f to Color(0xFFFFC370).copy(alpha = 0.07f * twilight),
                    1f to Color.Transparent
                )
            )
        }

        if (cloud > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF263B4A).copy(alpha = 0.13f * cloud),
                    0.42f to Color(0xFF536875).copy(alpha = 0.06f * cloud),
                    0.76f to Color.Transparent,
                    1f to Color.Transparent
                )
            )
        }

        if (rain > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF17364B).copy(alpha = 0.12f * rain),
                    0.56f to Color(0xFF244553).copy(alpha = 0.08f * rain),
                    1f to Color(0xFF07171F).copy(alpha = 0.06f * rain)
                )
            )
        }

        if (storm > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF0B1021).copy(alpha = 0.30f * storm),
                    0.42f to Color(0xFF182238).copy(alpha = 0.22f * storm),
                    1f to Color(0xFF050B12).copy(alpha = 0.16f * storm)
                )
            )
        }

        if (snow > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFCBDFE8).copy(alpha = 0.07f * snow),
                    0.46f to Color(0xFFE6F0F4).copy(alpha = 0.10f * snow),
                    1f to Color(0xFF9FB5BF).copy(alpha = 0.05f * snow)
                )
            )
        }

        if (fog > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.40f to Color(0xFFDCE5E6).copy(alpha = 0.05f * fog),
                    0.72f to Color(0xFFD5DFE0).copy(alpha = 0.09f * fog),
                    1f to Color(0xFFC8D5D8).copy(alpha = 0.06f * fog)
                )
            )
        }
    }
}

private fun sceneTargets(snapshot: LiveWeatherSnapshot?): SceneTransitionTargets {
    if (snapshot == null) return SceneTransitionTargets(0f, 0f, 0f, 0f, 0f, 0f, 0f)

    val code = snapshot.weatherCode
    val hour = snapshot.observedAt.substringAfter('T', "12:00").take(2).toIntOrNull() ?: 12
    val currentHour = snapshot.hourly24.firstOrNull()
    val precipitation = currentHour?.precipitationMm ?: 0.0
    val snowfall = currentHour?.snowfallCm ?: 0.0
    val rainProbability = (currentHour?.rainChance ?: 0).coerceIn(0, 100) / 100f

    val storm = when (code) {
        95 -> 0.68f
        96 -> 0.84f
        99 -> 1f
        else -> 0f
    }
    val rainCode = when (code) {
        51, 53, 55, 56, 57 -> 0.18f
        61 -> 0.30f
        63 -> 0.52f
        65, 66, 67 -> 0.82f
        80 -> 0.42f
        81 -> 0.64f
        82 -> 0.94f
        95, 96, 99 -> 0.72f
        else -> 0f
    }
    val measuredRain = (precipitation / 8.0).toFloat().coerceIn(0f, 1f)
    val rain = max(rainCode, max(measuredRain, rainProbability * 0.38f)).coerceIn(0f, 1f)

    val snowCode = when (code) {
        71 -> 0.34f
        73 -> 0.58f
        75, 77, 86 -> 0.92f
        85 -> 0.60f
        else -> 0f
    }
    val snow = max(snowCode, (snowfall / 1.8).toFloat()).coerceIn(0f, 1f)

    val cloud = max(snapshot.cloudCover.coerceIn(0, 100) / 100f, max(rain * 0.72f, storm * 0.88f))
        .coerceIn(0f, 1f)
    val night = if (snapshot.isDay) 0f else 1f
    val twilight = when (hour) {
        5, 19 -> 0.50f
        6, 18 -> 0.86f
        7, 17 -> 0.34f
        else -> 0f
    }
    val visibility = snapshot.visibilityKm ?: 30.0
    val fog = when {
        code == 45 || code == 48 -> 0.82f
        visibility < 0.8 -> 0.92f
        visibility < 2.0 -> 0.68f
        visibility < 5.0 -> 0.40f
        visibility < 8.0 -> 0.20f
        else -> 0f
    }

    return SceneTransitionTargets(
        night = night,
        twilight = twilight,
        cloud = cloud,
        rain = rain,
        storm = storm,
        snow = snow,
        fog = fog
    )
}
