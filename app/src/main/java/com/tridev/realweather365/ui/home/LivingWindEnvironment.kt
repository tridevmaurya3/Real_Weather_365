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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.WeatherCacheStore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

data class LivingWindPhysics(
    val windSpeedKmh: Int,
    val windGustKmh: Int,
    val windDirection: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val windStrength: Float,
    val gustStrength: Float,
    val horizontalFlow: Float
)

@Composable
fun LivingWindEnvironment(
    animationLevel: Int,
    modifier: Modifier = Modifier
) {
    val physics = rememberLivingWindPhysics()
    if (animationLevel <= 0) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawLivingWindWorld(physics, motion = 0.31f, bladeMotion = 0.18f)
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "living-wind-environment")
    val motion = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = windMotionDuration(physics.windSpeedKmh), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind-world-motion"
    )
    val blades = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = turbineDuration(physics.windSpeedKmh), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind-turbine-motion"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawLivingWindWorld(physics, motion.value, blades.value)
    }
}

@Composable
private fun rememberLivingWindPhysics(): LivingWindPhysics {
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
    return remember(snapshot) { calculateLivingWindPhysics(snapshot) }
}

private fun calculateLivingWindPhysics(snapshot: LiveWeatherSnapshot?): LivingWindPhysics {
    val wind = (snapshot?.windSpeed ?: 0).coerceAtLeast(0)
    val gust = (snapshot?.windGusts ?: wind).coerceAtLeast(wind)
    val direction = snapshot?.windDirection ?: 0
    val destination = ((direction + 180) % 360 + 360) % 360
    val radians = Math.toRadians(destination.toDouble())
    return LivingWindPhysics(
        windSpeedKmh = wind,
        windGustKmh = gust,
        windDirection = direction,
        weatherCode = snapshot?.weatherCode ?: 0,
        isDay = snapshot?.isDay ?: true,
        windStrength = (wind / 55f).coerceIn(0f, 1f),
        gustStrength = ((gust - wind).coerceAtLeast(0) / 38f).coerceIn(0f, 1f),
        horizontalFlow = sin(radians).toFloat()
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLivingWindWorld(
    physics: LivingWindPhysics,
    motion: Float,
    bladeMotion: Float
) {
    val snow = physics.weatherCode in setOf(71, 73, 75, 77, 85, 86)
    val wet = physics.weatherCode in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    val clearWorld = !snow && !wet

    if (physics.windSpeedKmh >= 24) {
        drawWindTraces(physics, motion)
    }

    if (clearWorld) {
        drawWindTurbines(physics, bladeMotion)
        drawWindResponsiveWater(physics, motion)
        drawWindVegetation(physics, motion)
        if (physics.isDay && physics.windSpeedKmh < 42) {
            drawBirdFlock(physics, motion)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWindResponsiveWater(
    physics: LivingWindPhysics,
    motion: Float
) {
    val strength = physics.windStrength
    val flow = if (physics.horizontalFlow == 0f) 1f else physics.horizontalFlow
    val baseY = size.height * 0.615f
    val dayAlpha = if (physics.isDay) 1f else 0.58f
    repeat(18) { index ->
        val depth = index / 17f
        val y = baseY + size.height * 0.155f * depth
        val phase = motion * 2f * PI.toFloat() + index * 0.73f
        val center = size.width * (0.50f + sin(phase) * (0.015f + strength * 0.028f) * flow)
        val half = size.width * (0.07f + depth * 0.16f + strength * 0.05f)
        val alpha = (0.035f + depth * 0.055f + strength * 0.075f) * dayAlpha
        drawLine(
            color = Color(0xFFD9F2F5).copy(alpha = alpha.coerceAtMost(0.18f)),
            start = Offset(center - half, y),
            end = Offset(center + half, y + sin(phase * 1.4f) * size.height * 0.002f),
            strokeWidth = 0.8f + depth * 1.4f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWindVegetation(
    physics: LivingWindPhysics,
    motion: Float
) {
    val baseline = size.height * 0.805f
    val flow = physics.horizontalFlow
    val strength = physics.windStrength
    val gust = physics.gustStrength
    val baseColor = if (physics.isDay) Color(0xFF173D31) else Color(0xFF0B2320)

    repeat(42) { index ->
        val x = size.width * (index / 41f)
        val height = size.height * (0.025f + (index % 6) * 0.006f)
        val localPhase = motion * 2f * PI.toFloat() + index * 0.49f
        val oscillation = sin(localPhase) * size.width * (0.002f + strength * 0.004f)
        val lean = flow * height * (0.08f + strength * 0.42f + gust * 0.18f) + oscillation
        drawLine(
            color = baseColor.copy(alpha = 0.52f + (index % 4) * 0.08f),
            start = Offset(x, baseline),
            end = Offset(x + lean, baseline - height),
            strokeWidth = 1.2f + (index % 3) * 0.45f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWindTurbines(
    physics: LivingWindPhysics,
    bladeMotion: Float
) {
    val alpha = if (physics.isDay) 0.42f else 0.20f
    val towerColor = Color(0xFFDDE9EB).copy(alpha = alpha)
    val bladeColor = Color(0xFFE8F3F4).copy(alpha = (alpha * 1.08f).coerceAtMost(0.5f))
    val spin = bladeMotion * 2f * PI.toFloat()

    listOf(0.78f to 0.092f, 0.88f to 0.072f).forEachIndexed { index, (xFrac, hFrac) ->
        val groundY = size.height * (0.605f + index * 0.010f)
        val hub = Offset(size.width * xFrac, groundY - size.height * hFrac)
        drawLine(
            color = towerColor,
            start = Offset(hub.x, groundY),
            end = hub,
            strokeWidth = 2.0f - index * 0.35f
        )
        val radius = size.height * hFrac * 0.34f
        repeat(3) { blade ->
            val angle = spin + blade * (2f * PI.toFloat() / 3f)
            drawLine(
                color = bladeColor,
                start = hub,
                end = Offset(hub.x + cos(angle) * radius, hub.y + sin(angle) * radius),
                strokeWidth = 1.7f - index * 0.25f
            )
        }
        drawCircle(bladeColor, radius = 2.4f - index * 0.4f, center = hub)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWindTraces(
    physics: LivingWindPhysics,
    motion: Float
) {
    val strength = physics.windStrength
    val flow = if (physics.horizontalFlow == 0f) 1f else physics.horizontalFlow
    repeat(9) { index ->
        val width = size.width * (0.07f + (index % 3) * 0.035f)
        val raw = size.width * ((index * 0.137f + motion * (0.45f + strength * 1.25f)) % 1.12f)
        val x = if (flow >= 0f) raw - width else size.width - raw
        val y = size.height * (0.17f + (index % 6) * 0.07f)
        drawLine(
            color = Color.White.copy(alpha = (0.025f + strength * 0.055f).coerceAtMost(0.08f)),
            start = Offset(x, y),
            end = Offset(x + width * flow, y - size.height * 0.006f),
            strokeWidth = 1.0f + strength
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBirdFlock(
    physics: LivingWindPhysics,
    motion: Float
) {
    val direction = if (physics.horizontalFlow >= 0f) 1f else -1f
    repeat(4) { index ->
        val progress = (motion * (0.60f + index * 0.06f) + index * 0.21f) % 1f
        val x = if (direction > 0) size.width * progress else size.width * (1f - progress)
        val y = size.height * (0.19f + index * 0.026f + sin(progress * 2f * PI.toFloat() + index) * 0.012f)
        val wing = size.width * (0.008f + index * 0.001f)
        val flap = sin(progress * 9f * PI.toFloat()) * size.height * 0.004f
        val c = Color(0xFF17282C).copy(alpha = 0.48f)
        drawLine(c, Offset(x - wing, y + flap), Offset(x, y), 1.6f)
        drawLine(c, Offset(x, y), Offset(x + wing, y - flap), 1.6f)
    }
}

private fun windMotionDuration(windSpeedKmh: Int): Int {
    val factor = (0.55f + windSpeedKmh.coerceIn(0, 90) / 35f).coerceIn(0.55f, 3.0f)
    return (9_000 / factor).toInt().coerceIn(2_600, 14_000)
}

private fun turbineDuration(windSpeedKmh: Int): Int {
    val factor = (0.30f + windSpeedKmh.coerceIn(0, 90) / 18f).coerceIn(0.30f, 4.4f)
    return (8_500 / factor).toInt().coerceIn(1_700, 22_000)
}
