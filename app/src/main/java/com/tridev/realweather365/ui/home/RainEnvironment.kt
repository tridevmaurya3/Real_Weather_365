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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RainEnvironment(modifier: Modifier = Modifier) {
    val physics = rememberPrecipitationPhysics(storm = false)
    val intensity = physics.intensity.coerceIn(0.08f, 1f)
    val transition = rememberInfiniteTransition(label = "provider-rain-environment")
    val rainFlow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1850 - intensity * 1050).toInt().coerceIn(650, 1800),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "provider-rain-flow"
    )
    val surfaceFlow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wet-surface-flow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawProviderRainSky(intensity)
        drawRainHorizon(intensity)
        drawRainCityWorld(intensity)
        drawWetRoadWorld(intensity, surfaceFlow.value)
        drawRainStreetLights(intensity)
        drawWindBentTree(physics)
        drawProviderRain(physics, rainFlow.value)
        drawRainSplashes(physics, rainFlow.value)
        drawRainAtmosphere(intensity)
    }
}

private fun DrawScope.drawProviderRainSky(intensity: Float) {
    val darkness = intensity.coerceIn(0f, 1f)
    drawRect(
        brush = Brush.verticalGradient(
            0f to mixRain(Color(0xFF314A5B), Color(0xFF101C28), darkness * 0.72f),
            0.33f to mixRain(Color(0xFF526873), Color(0xFF243441), darkness * 0.66f),
            0.62f to mixRain(Color(0xFF607078), Color(0xFF2B3A43), darkness * 0.55f),
            1f to Color(0xFF0B1B23)
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFB9D3D8).copy(alpha = 0.10f * (1f - darkness * 0.48f)),
                Color.Transparent
            ),
            center = Offset(size.width * 0.48f, size.height * 0.39f),
            radius = size.width * 0.82f
        )
    )
}

private fun DrawScope.drawRainHorizon(intensity: Float) {
    val y = size.height * 0.45f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFB8CDD1).copy(alpha = 0.06f + intensity * 0.07f),
                Color.Transparent
            ),
            startY = y - size.height * 0.09f,
            endY = y + size.height * 0.10f
        )
    )
}

private fun DrawScope.drawRainCityWorld(intensity: Float) {
    val horizon = size.height * 0.51f
    val buildings = listOf(
        0.00f to 0.13f, 0.10f to 0.18f, 0.21f to 0.12f, 0.31f to 0.22f,
        0.47f to 0.14f, 0.57f to 0.19f, 0.72f to 0.15f, 0.84f to 0.23f, 0.94f to 0.16f
    )
    buildings.forEachIndexed { index, pair ->
        val x = size.width * pair.first
        val width = size.width * if (index % 2 == 0) 0.12f else 0.10f
        val height = size.height * pair.second
        val top = horizon - height
        val base = mixRain(Color(0xFF172A33), Color(0xFF08151C), intensity * 0.72f)
        drawRect(
            color = if (index % 3 == 0) mixRain(base, Color.Black, 0.16f) else base,
            topLeft = Offset(x, top),
            size = Size(width, height + size.height * 0.11f)
        )
        repeat(4) { row ->
            repeat(2) { col ->
                if ((row + col + index) % 3 == 0) {
                    drawRect(
                        color = Color(0xFFFFD58A).copy(alpha = 0.54f - intensity * 0.08f),
                        topLeft = Offset(
                            x + width * (0.20f + col * 0.46f),
                            top + size.height * (0.028f + row * 0.032f)
                        ),
                        size = Size(width * 0.10f, size.height * 0.010f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawWetRoadWorld(intensity: Float, progress: Float) {
    val top = size.height * 0.54f
    val road = Path().apply {
        moveTo(size.width * 0.27f, top)
        lineTo(size.width * 0.73f, top)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        road,
        brush = Brush.verticalGradient(
            colors = listOf(
                mixRain(Color(0xFF34474D), Color(0xFF26363E), intensity * 0.55f),
                Color(0xFF10242B),
                Color(0xFF06161C)
            ),
            startY = top,
            endY = size.height
        )
    )

    val wetness = (0.16f + intensity * 0.54f).coerceIn(0f, 0.72f)
    repeat(22) { index ->
        val perspective = index / 21f
        val y = top + perspective * (size.height - top)
        val wave = sin(progress * 2f * PI.toFloat() + index * 0.71f)
        val center = size.width * (0.50f + wave * (0.025f + perspective * 0.035f))
        val half = size.width * (0.025f + perspective * 0.15f)
        drawLine(
            color = Color(0xFFB6E5EA).copy(alpha = wetness * (0.18f + perspective * 0.24f)),
            start = Offset(center - half, y),
            end = Offset(center + half, y),
            strokeWidth = 0.9f + perspective * 2.5f
        )
    }

    listOf(0.27f, 0.73f).forEach { xRatio ->
        repeat(10) { index ->
            val y = top + size.height * (0.032f + index * 0.036f)
            val half = size.width * (0.014f + index * 0.009f)
            drawLine(
                color = Color(0xFFFFBA65).copy(alpha = (0.16f + intensity * 0.16f - index * 0.012f).coerceAtLeast(0.025f)),
                start = Offset(size.width * xRatio - half, y),
                end = Offset(size.width * xRatio + half, y),
                strokeWidth = 1.7f + intensity
            )
        }
    }
}

private fun DrawScope.drawRainStreetLights(intensity: Float) {
    val roadTop = size.height * 0.52f
    listOf(0.26f, 0.74f).forEach { xRatio ->
        val x = size.width * xRatio
        val top = roadTop - size.height * 0.08f
        val bottom = roadTop + size.height * 0.12f
        drawLine(Color(0xFF10191D), Offset(x, top), Offset(x, bottom), size.width * 0.007f)
        val glow = 0.62f + intensity * 0.20f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE1A0).copy(alpha = glow),
                    Color(0xFFFFB24A).copy(alpha = glow * 0.28f),
                    Color.Transparent
                ),
                center = Offset(x, top),
                radius = size.width * 0.10f
            ),
            radius = size.width * 0.10f,
            center = Offset(x, top)
        )
        drawCircle(Color(0xFFFFD17C), size.width * 0.011f, Offset(x, top))
    }
}

private fun DrawScope.drawWindBentTree(physics: PrecipitationPhysics) {
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(destination * PI / 180.0).toFloat()
    val bend = windX * (physics.windSpeedKmh / 75f).coerceIn(0f, 0.34f)
    val baseX = size.width * 0.94f
    val baseY = size.height * 0.68f
    val crownX = size.width * (0.86f + bend * 0.16f)
    drawLine(
        color = Color(0xFF071713),
        start = Offset(baseX, baseY),
        end = Offset(crownX, size.height * 0.28f),
        strokeWidth = size.width * 0.025f
    )
    repeat(15) { index ->
        val cx = size.width * (0.75f + (index % 5) * 0.050f) + bend * size.width * 0.08f
        val cy = size.height * (0.22f + (index / 5) * 0.052f)
        drawCircle(
            color = Color(0xFF0A2A21).copy(alpha = 0.94f),
            radius = size.width * (0.054f + (index % 3) * 0.010f),
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawProviderRain(physics: PrecipitationPhysics, progress: Float) {
    val intensity = physics.intensity.coerceIn(0.06f, 1f)
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(destination * PI / 180.0).toFloat()
    val windStrength = (physics.windSpeedKmh / 65f).coerceIn(0f, 1.25f)
    val slantFar = windX * size.width * (0.008f + 0.026f * windStrength)
    val slantNear = windX * size.width * (0.014f + 0.044f * windStrength)
    val farCount = (38 + intensity * 118).toInt()
    val nearCount = (10 + intensity * 42).toInt()

    repeat(farCount) { index ->
        val seedX = ((index * 37 + 11) % 211) / 211f
        val seedY = ((index * 53 + 7) % 223) / 223f
        val y = ((seedY + progress * (0.72f + intensity * 0.54f)) % 1.08f) * size.height
        val x = ((seedX + progress * windX * 0.055f * windStrength) % 1.12f) * size.width
        val length = size.height * (0.014f + intensity * 0.018f + (index % 4) * 0.0025f)
        drawLine(
            color = Color(0xFFC9E9EF).copy(alpha = 0.12f + intensity * 0.20f),
            start = Offset(x, y),
            end = Offset(x + slantFar, y + length),
            strokeWidth = 0.8f + intensity * 0.45f
        )
    }

    repeat(nearCount) { index ->
        val seedX = ((index * 61 + 17) % 197) / 197f
        val seedY = ((index * 47 + 9) % 199) / 199f
        val y = ((seedY + progress * (1.0f + intensity * 0.72f)) % 1.10f) * size.height
        val x = ((seedX + progress * windX * 0.075f * windStrength) % 1.14f) * size.width
        val length = size.height * (0.026f + intensity * 0.030f + (index % 5) * 0.003f)
        drawLine(
            color = Color(0xFFE8FAFC).copy(alpha = 0.24f + intensity * 0.32f),
            start = Offset(x, y),
            end = Offset(x + slantNear, y + length),
            strokeWidth = 1.2f + intensity * 0.9f
        )
    }
}

private fun DrawScope.drawRainSplashes(physics: PrecipitationPhysics, progress: Float) {
    val intensity = physics.intensity.coerceIn(0f, 1f)
    if (intensity < 0.12f) return
    val count = (8 + intensity * 23).toInt()
    repeat(count) { index ->
        val x = size.width * (((index * 29 + 7) % 101) / 101f)
        val y = size.height * (0.61f + ((index * 17) % 31) / 100f)
        val pulse = (progress * (1.1f + intensity) + index * 0.117f) % 1f
        val radiusX = size.width * (0.008f + intensity * 0.020f) * pulse
        val alpha = (0.28f * intensity * (1f - pulse)).coerceAtLeast(0f)
        drawOval(
            color = Color(0xFFCDEFF2).copy(alpha = alpha),
            topLeft = Offset(x - radiusX, y),
            size = Size(radiusX * 2f, size.height * 0.003f * (0.6f + pulse))
        )
        if (intensity > 0.55f && pulse < 0.22f) {
            drawLine(
                color = Color(0xFFE7FAFC).copy(alpha = 0.20f * intensity),
                start = Offset(x, y),
                end = Offset(x + (index % 3 - 1) * size.width * 0.010f, y - size.height * 0.010f),
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawRainAtmosphere(intensity: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x12081218),
                Color.Transparent,
                Color(0xFF021016).copy(alpha = 0.32f + intensity * 0.16f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
}

private fun mixRain(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}
