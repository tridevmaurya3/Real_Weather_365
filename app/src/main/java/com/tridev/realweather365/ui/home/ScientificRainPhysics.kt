package com.tridev.realweather365.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

data class RainMicrophysics(
    val medianDiameterMm: Float,
    val terminalVelocityMs: Float,
    val visibleDropDensity: Int,
    val windDrift: Float,
    val splashEnergy: Float,
    val surfaceWetness: Float,
    val mistStrength: Float
)

object RainMicrophysicsEngine {
    fun calculate(physics: PrecipitationPhysics): RainMicrophysics {
        val rate = physics.precipitationMm.toFloat().coerceIn(0f, 35f)
        val codeDiameter = when (physics.weatherCode) {
            51, 53, 55, 56, 57 -> 0.34f
            61, 80 -> 0.85f
            63, 81, 95 -> 1.45f
            65, 67, 82, 96, 99 -> 2.35f
            else -> 0.65f
        }
        val diameter = (codeDiameter + sqrt(rate) * 0.17f).coerceIn(0.22f, 3.8f)
        // Atlas-type still-air terminal-speed fit; D is diameter in millimetres.
        val velocity = (9.65f - 10.3f * exp(-0.6f * diameter)).coerceIn(0.7f, 9.8f)
        val intensity = physics.intensity.coerceIn(0f, 1f)
        val wind = physics.windSpeedKmh.coerceIn(0, 120) / 120f
        val kinetic = (diameter * diameter * diameter * velocity * velocity / 760f).coerceIn(0.04f, 1f)
        return RainMicrophysics(
            medianDiameterMm = diameter,
            terminalVelocityMs = velocity,
            visibleDropDensity = (34 + intensity * 185f).toInt(),
            windDrift = wind,
            splashEnergy = (kinetic * 0.55f + intensity * 0.45f).coerceIn(0f, 1f),
            surfaceWetness = (intensity * 0.78f + (rate / 20f) * 0.22f).coerceIn(0f, 1f),
            mistStrength = ((intensity - 0.48f) * 1.35f + wind * 0.18f).coerceIn(0f, 0.72f)
        )
    }
}

internal fun DrawScope.drawScientificRainField(
    physics: PrecipitationPhysics,
    progress: Float,
    foregroundStrength: Float = 1f
) {
    val micro = RainMicrophysicsEngine.calculate(physics)
    val direction = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(direction * PI / 180.0).toFloat()
    val speed = micro.terminalVelocityMs / 9.8f
    val count = (micro.visibleDropDensity * foregroundStrength).toInt().coerceAtLeast(1)

    repeat(count) { index ->
        val depth = ((index * 67 + 19) % 101) / 100f
        val seedX = ((index * 43 + 13) % 257) / 257f
        val seedY = ((index * 71 + 29) % 263) / 263f
        val travel = 0.58f + speed * (0.56f + depth * 0.42f)
        val y = ((seedY + progress * travel) % 1.08f) * size.height
        val drift = windX * micro.windDrift * (0.035f + depth * 0.075f)
        val x = positiveModulo(seedX + progress * drift + depth * 0.013f, 1.08f) * size.width
        val length = size.height * (0.006f + speed * 0.014f + depth * 0.031f)
        val slant = windX * micro.windDrift * length * 1.85f
        val alpha = (0.08f + physics.intensity * 0.20f + depth * 0.26f).coerceAtMost(0.62f)
        val width = 0.55f + depth * 1.55f + micro.medianDiameterMm * 0.10f
        drawLine(
            color = Color(0xFFDDF7FA).copy(alpha = alpha),
            start = Offset(x, y),
            end = Offset(x + slant, y + length),
            strokeWidth = width
        )

        // Close drops reveal aerodynamic flattening; they are not tear-shaped icons.
        if (depth > 0.87f && index % 7 == 0) {
            val radius = size.width * (0.0021f + micro.medianDiameterMm * 0.00042f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = alpha * 0.72f), Color(0xFF88C5D2).copy(alpha = alpha * 0.20f), Color.Transparent),
                    center = Offset(x, y),
                    radius = radius * 1.7f
                ),
                topLeft = Offset(x - radius * 1.18f, y - radius * 0.76f),
                size = Size(radius * 2.36f, radius * 1.52f)
            )
        }
    }
}

internal fun DrawScope.drawScientificImpactField(physics: PrecipitationPhysics, progress: Float, surfaceTop: Float) {
    val micro = RainMicrophysicsEngine.calculate(physics)
    if (physics.intensity < 0.1f) return
    val count = (6 + micro.splashEnergy * 28f).toInt()
    repeat(count) { index ->
        val x = size.width * (((index * 47 + 11) % 109) / 109f)
        val depth = ((index * 23) % 37) / 37f
        val y = surfaceTop + (size.height - surfaceTop) * (0.08f + depth * 0.82f)
        val pulse = positiveModulo(progress * (1.15f + micro.terminalVelocityMs / 9.8f) + index * 0.137f, 1f)
        val perspective = ((y - surfaceTop) / (size.height - surfaceTop)).coerceIn(0f, 1f)
        val rx = size.width * (0.004f + perspective * 0.020f) * pulse * (0.6f + micro.splashEnergy)
        val ry = rx * (0.12f + perspective * 0.08f)
        drawOval(
            color = Color(0xFFD9F5F7).copy(alpha = (1f - pulse) * 0.31f * micro.splashEnergy),
            topLeft = Offset(x - rx, y - ry),
            size = Size(rx * 2f, ry * 2f)
        )
        if (pulse < 0.18f && micro.splashEnergy > 0.35f) {
            repeat(3) { shard ->
                val dx = (shard - 1) * size.width * 0.006f * (0.5f + perspective)
                drawLine(
                    color = Color(0xFFE9FCFD).copy(alpha = 0.28f * micro.splashEnergy),
                    start = Offset(x, y),
                    end = Offset(x + dx, y - size.height * 0.008f * micro.splashEnergy),
                    strokeWidth = 0.7f + perspective
                )
            }
        }
    }
}

internal fun DrawScope.drawLensDroplets(physics: PrecipitationPhysics, progress: Float) {
    val micro = RainMicrophysicsEngine.calculate(physics)
    if (physics.intensity < 0.38f) return
    val count = (2 + physics.intensity * 7f).toInt()
    repeat(count) { index ->
        val x = size.width * (0.08f + ((index * 41 + 17) % 83) / 100f)
        val baseY = size.height * (0.08f + ((index * 29 + 13) % 54) / 100f)
        val slide = if (index % 3 == 0) positiveModulo(progress * (0.08f + micro.surfaceWetness * 0.09f) + index * 0.11f, 0.22f) else 0f
        val y = baseY + size.height * slide
        val radius = size.width * (0.010f + (index % 4) * 0.0035f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEFFFFF).copy(alpha = 0.18f), Color(0xFF264A59).copy(alpha = 0.10f), Color.Transparent),
                center = Offset(x - radius * 0.25f, y - radius * 0.28f),
                radius = radius * 1.2f
            ),
            topLeft = Offset(x - radius, y - radius * 0.78f),
            size = Size(radius * 2f, radius * 1.56f)
        )
        drawCircle(Color.White.copy(alpha = 0.31f), radius * 0.12f, Offset(x - radius * 0.34f, y - radius * 0.28f))
    }
}

private fun positiveModulo(value: Float, divisor: Float): Float = ((value % divisor) + divisor) % divisor
