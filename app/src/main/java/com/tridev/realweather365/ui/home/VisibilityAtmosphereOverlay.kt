package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.WeatherCacheStore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.delay

data class AtmosphereVisibilityPhysics(
    val visibilityKm: Double,
    val humidity: Int,
    val pm25: Double,
    val aqi: Int,
    val windSpeedKmh: Int,
    val windDirection: Int,
    val weatherCode: Int,
    val fogDensity: Float,
    val hazeDensity: Float,
    val contrastLoss: Float
)

@Composable
fun VisibilityAtmosphereOverlay(
    animationLevel: Int,
    modifier: Modifier = Modifier
) {
    LivingWindEnvironment(
        animationLevel = animationLevel,
        modifier = modifier
    )

    val physics = rememberAtmosphereVisibilityPhysics()
    if (physics.fogDensity < 0.035f && physics.hazeDensity < 0.035f) return

    val transition = rememberInfiniteTransition(label = "real-visibility-atmosphere")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = visibilityMotionDuration(physics.windSpeedKmh),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "visibility-drift"
    )
    val motion = if (animationLevel <= 0) 0.37f else progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val fog = physics.fogDensity
        val haze = physics.hazeDensity
        val lowVisibility = physics.contrastLoss
        val destination = ((physics.windDirection + 180) % 360 + 360) % 360
        val radians = destination / 180.0 * PI
        val flowX = sin(radians).toFloat()
        val flowY = (-cos(radians)).toFloat()

        if (haze > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFB7B09D).copy(alpha = haze * 0.11f),
                    0.36f to Color(0xFFC6B99F).copy(alpha = haze * 0.20f),
                    0.66f to Color(0xFFB2A88F).copy(alpha = haze * 0.13f),
                    1f to Color.Transparent
                )
            )
        }

        if (fog > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFD9E3E5).copy(alpha = fog * 0.12f),
                    0.28f to Color(0xFFE4ECEC).copy(alpha = fog * 0.25f),
                    0.58f to Color(0xFFD7E1E1).copy(alpha = fog * 0.34f),
                    0.82f to Color(0xFFC9D5D7).copy(alpha = fog * 0.24f),
                    1f to Color(0xFFB6C5C9).copy(alpha = fog * 0.16f)
                )
            )

            val bandCount = if (fog > 0.62f) 7 else 5
            repeat(bandCount) { index ->
                val width = size.width * (0.62f + (index % 3) * 0.18f)
                val baseX = size.width * ((index * 0.271f + 0.04f) % 1f)
                val travel = size.width * (0.92f + index * 0.08f)
                val rawX = baseX + motion * travel * flowX
                val x = wrapVisibility(rawX, size.width, width)
                val yBase = size.height * (0.22f + index * 0.085f)
                val y = yBase + motion * size.height * 0.038f * flowY
                val alpha = (fog * (0.13f + (index % 3) * 0.035f)).coerceAtMost(0.30f)
                drawOval(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFE8EEEE).copy(alpha = alpha),
                            Color(0xFFD3DDDF).copy(alpha = alpha * 0.90f),
                            Color.Transparent
                        ),
                        startX = x,
                        endX = x + width
                    ),
                    topLeft = Offset(x, y),
                    size = Size(width, size.height * (0.07f + (index % 2) * 0.025f))
                )
            }

            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.52f to Color.Transparent,
                    0.72f to Color(0xFFDCE5E6).copy(alpha = fog * 0.20f),
                    1f to Color(0xFFCBD8DA).copy(alpha = fog * 0.34f)
                )
            )
        }

        if (lowVisibility > 0.03f) {
            drawRect(
                color = Color(0xFFDCE4E5).copy(alpha = lowVisibility * 0.12f)
            )
        }
    }
}

@Composable
private fun rememberAtmosphereVisibilityPhysics(): AtmosphereVisibilityPhysics {
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
    return remember(snapshot) { calculateAtmosphereVisibility(snapshot) }
}

private fun calculateAtmosphereVisibility(snapshot: LiveWeatherSnapshot?): AtmosphereVisibilityPhysics {
    val visibility = (snapshot?.visibilityKm ?: 30.0).coerceIn(0.05, 80.0)
    val humidity = (snapshot?.humidity ?: 0).coerceIn(0, 100)
    val pm25 = (snapshot?.pm25 ?: 0.0).coerceAtLeast(0.0)
    val aqi = (snapshot?.aqi ?: 0).coerceAtLeast(0)
    val code = snapshot?.weatherCode ?: 0

    val visibilityFog = when {
        visibility < 0.35 -> 0.94f
        visibility < 0.75 -> 0.82f
        visibility < 1.5 -> 0.68f
        visibility < 3.0 -> 0.51f
        visibility < 5.0 -> 0.34f
        visibility < 8.0 -> 0.18f
        else -> 0f
    }
    val fogCodeFloor = if (code == 45 || code == 48) 0.66f else 0f
    val humidityBoost = when {
        humidity >= 98 -> 0.16f
        humidity >= 95 -> 0.11f
        humidity >= 91 -> 0.06f
        else -> 0f
    }
    val fogDensity = max(fogCodeFloor, visibilityFog + humidityBoost).coerceIn(0f, 0.94f)

    val pmFactor = when {
        pm25 >= 150 -> 0.82f
        pm25 >= 90 -> 0.64f
        pm25 >= 55 -> 0.48f
        pm25 >= 35 -> 0.30f
        pm25 >= 20 -> 0.15f
        else -> 0f
    }
    val aqiFactor = when {
        aqi >= 250 -> 0.75f
        aqi >= 180 -> 0.58f
        aqi >= 120 -> 0.38f
        aqi >= 90 -> 0.18f
        else -> 0f
    }
    val visibilitySupport = when {
        visibility < 4 -> 0.90f
        visibility < 8 -> 0.72f
        visibility < 12 -> 0.52f
        visibility < 18 -> 0.32f
        else -> 0.18f
    }
    val pollutionSignal = max(pmFactor, aqiFactor)
    val hazeDensity = (pollutionSignal * visibilitySupport * if (fogDensity > 0.60f) 0.35f else 1f)
        .coerceIn(0f, 0.78f)

    val contrastLoss = when {
        visibility < 0.5 -> 0.92f
        visibility < 1.0 -> 0.80f
        visibility < 2.0 -> 0.66f
        visibility < 5.0 -> 0.43f
        visibility < 10.0 -> 0.20f
        else -> max(fogDensity * 0.25f, hazeDensity * 0.18f)
    }.coerceIn(0f, 0.92f)

    return AtmosphereVisibilityPhysics(
        visibilityKm = visibility,
        humidity = humidity,
        pm25 = pm25,
        aqi = aqi,
        windSpeedKmh = snapshot?.windSpeed ?: 0,
        windDirection = snapshot?.windDirection ?: 0,
        weatherCode = code,
        fogDensity = fogDensity,
        hazeDensity = hazeDensity,
        contrastLoss = contrastLoss
    )
}

private fun visibilityMotionDuration(windSpeedKmh: Int): Int {
    val factor = (0.55f + windSpeedKmh.coerceIn(0, 80) / 36f).coerceIn(0.55f, 2.4f)
    return (78_000 / factor).toInt().coerceIn(24_000, 120_000)
}

private fun wrapVisibility(value: Float, width: Float, itemWidth: Float): Float {
    val span = width + itemWidth * 2f
    var normalized = (value + itemWidth) % span
    if (normalized < 0f) normalized += span
    return normalized - itemWidth
}
