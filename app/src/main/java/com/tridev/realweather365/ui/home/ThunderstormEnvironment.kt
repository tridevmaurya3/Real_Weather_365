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
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ThunderstormEnvironment(animationLevel: Int = 2, modifier: Modifier = Modifier) {
    val physics = rememberPrecipitationPhysics(storm = true)
    val intensity = physics.intensity.coerceIn(0.45f, 1f)
    val lightningPotential = physics.lightningPotential.coerceIn(0.18f, 1f)
    val transition = rememberInfiniteTransition(label = "provider-thunderstorm")

    val rainFlow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1050 - intensity * 480).toInt().coerceIn(520, 950),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "storm-provider-rain"
    )
    val lightningCycle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (9200 - lightningPotential * 6500).toInt().coerceIn(2400, 8200),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cape-lightning-cycle"
    )
    val waterFlow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storm-reflection-flow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val rainProgress = if (animationLevel <= 0) 0.43f else rainFlow.value
        val flash = if (animationLevel <= 0) 0f else providerLightningFlash(lightningCycle.value, lightningPotential)
        drawProviderStormSky(intensity, flash)
        drawStormHorizon(intensity, flash)
        drawStormCityWorld(intensity, flash)
        drawStormWetSurface(intensity, flash, waterFlow.value)
        if (flash > 0.13f) drawProviderLightningBolt(flash, lightningPotential)
        drawScientificRainField(physics, rainProgress, foregroundStrength = 1.18f)
        if (physics.hail) drawProviderHail(physics, rainProgress)
        drawScientificImpactField(physics, rainProgress, size.height * 0.64f)
        drawLensDroplets(physics, rainProgress)
        drawStormExposure(intensity, flash)
    }
}

private fun providerLightningFlash(phase: Float, potential: Float): Float {
    if (potential < 0.16f) return 0f
    val first = triangularPulse(phase, 0.60f, 0.018f)
    val second = triangularPulse(phase, 0.655f, 0.026f) * (0.52f + potential * 0.32f)
    val distant = if (potential > 0.72f) triangularPulse(phase, 0.31f, 0.020f) * 0.42f else 0f
    return maxOf(first, second, distant).coerceIn(0f, 1f) * (0.55f + potential * 0.45f)
}

private fun triangularPulse(value: Float, center: Float, halfWidth: Float): Float {
    val distance = abs(value - center)
    return if (distance >= halfWidth) 0f else 1f - distance / halfWidth
}

private fun DrawScope.drawProviderStormSky(intensity: Float, flash: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to mixStorm(Color(0xFF101D2B), Color(0xFF040A12), intensity * 0.78f),
            0.28f to mixStorm(Color(0xFF24394D), Color(0xFF111E2B), intensity * 0.72f),
            0.55f to mixStorm(Color(0xFF314759), Color(0xFF1B2C3A), intensity * 0.66f),
            1f to Color(0xFF06131B)
        )
    )
    if (flash > 0f) {
        drawRect(Color(0xFFB8D5F4).copy(alpha = flash * (0.16f + intensity * 0.10f)))
    }
}

private fun DrawScope.drawStormHorizon(intensity: Float, flash: Float) {
    val horizon = size.height * 0.47f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF88A9BE).copy(alpha = 0.06f + intensity * 0.07f + flash * 0.20f),
                Color.Transparent
            ),
            startY = horizon - size.height * 0.10f,
            endY = horizon + size.height * 0.11f
        )
    )
}

private fun DrawScope.drawStormCityWorld(intensity: Float, flash: Float) {
    val ground = size.height * 0.61f
    val buildings = listOf(
        0.01f to 0.16f, 0.09f to 0.23f, 0.18f to 0.14f, 0.26f to 0.27f,
        0.36f to 0.19f, 0.45f to 0.31f, 0.57f to 0.21f, 0.67f to 0.28f,
        0.78f to 0.18f, 0.87f to 0.25f, 0.95f to 0.16f
    )
    buildings.forEachIndexed { index, pair ->
        val width = size.width * (0.075f + (index % 3) * 0.016f)
        val height = size.height * pair.second
        val left = size.width * pair.first
        val top = ground - height
        val litFace = mixStorm(Color(0xFF09151D), Color(0xFF253849), flash * 0.50f)
        drawRect(
            color = if (index % 2 == 0) litFace else mixStorm(Color(0xFF0B1B24), Color(0xFF304458), flash * 0.44f),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )
        repeat(7) { row ->
            repeat(3) { col ->
                if ((row + col + index) % 3 == 0) {
                    drawRect(
                        color = Color(0xFFFFC96B).copy(alpha = 0.48f + flash * 0.12f - intensity * 0.05f),
                        topLeft = Offset(
                            left + width * (0.16f + col * 0.27f),
                            top + height * (0.13f + row * 0.105f)
                        ),
                        size = Size(width * 0.07f, height * 0.025f)
                    )
                }
            }
        }
    }
    drawRect(
        color = mixStorm(Color(0xFF07141B), Color(0xFF182A36), flash * 0.30f),
        topLeft = Offset(0f, ground),
        size = Size(size.width, size.height * 0.07f)
    )
}

private fun DrawScope.drawStormWetSurface(intensity: Float, flash: Float, progress: Float) {
    val top = size.height * 0.64f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                mixStorm(Color(0xFF0B2939), Color(0xFF385A70), flash * 0.34f),
                mixStorm(Color(0xFF071E2A), Color(0xFF29495D), flash * 0.26f),
                Color(0xFF031018)
            ),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )

    repeat(26) { index ->
        val phase = progress * 2f * PI.toFloat() + index * 0.61f
        val y = top + size.height * (0.014f + index * 0.011f)
        val center = size.width * (0.52f + sin(phase) * 0.12f)
        val half = size.width * (0.030f + (index % 6) * 0.021f)
        drawLine(
            color = Color(0xFF9BC7DB).copy(alpha = 0.07f + intensity * 0.07f + flash * 0.13f),
            start = Offset(center - half, y),
            end = Offset(center + half, y),
            strokeWidth = 1.1f + (index % 2) * 0.5f
        )
    }

    if (flash > 0.08f) {
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFDCEBFA).copy(alpha = flash * 0.22f),
                    Color.Transparent
                ),
                startY = top,
                endY = size.height
            ),
            topLeft = Offset(size.width * 0.34f, top),
            size = Size(size.width * 0.34f, size.height - top)
        )
    }
}

private fun DrawScope.drawProviderLightningBolt(flash: Float, potential: Float) {
    val centerX = size.width * (0.54f + (potential - 0.5f) * 0.10f)
    val bolt = Path().apply {
        moveTo(centerX, size.height * 0.13f)
        lineTo(centerX - size.width * 0.040f, size.height * 0.25f)
        lineTo(centerX + size.width * 0.018f, size.height * 0.245f)
        lineTo(centerX - size.width * 0.060f, size.height * 0.40f)
        lineTo(centerX - size.width * 0.010f, size.height * 0.335f)
        lineTo(centerX - size.width * 0.050f, size.height * 0.345f)
        close()
    }
    drawPath(bolt, Color.White.copy(alpha = 0.86f * flash))
    drawLine(
        color = Color(0xFFA7CDFF).copy(alpha = 0.36f * flash),
        start = Offset(centerX - size.width * 0.01f, size.height * 0.14f),
        end = Offset(centerX - size.width * 0.055f, size.height * 0.40f),
        strokeWidth = 7f + potential * 3f
    )

    if (potential > 0.70f) {
        drawLine(
            color = Color(0xFFD9EAFF).copy(alpha = 0.50f * flash),
            start = Offset(centerX - size.width * 0.030f, size.height * 0.28f),
            end = Offset(centerX + size.width * 0.055f, size.height * 0.35f),
            strokeWidth = 1.6f
        )
    }
}

private fun DrawScope.drawProviderStormRain(physics: PrecipitationPhysics, progress: Float) {
    val intensity = physics.intensity.coerceIn(0.42f, 1f)
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(destination * PI / 180.0).toFloat()
    val gustStrength = (physics.windGustKmh / 80f).coerceIn(0.20f, 1.50f)
    val slantFar = windX * size.width * (0.018f + gustStrength * 0.035f)
    val slantNear = windX * size.width * (0.028f + gustStrength * 0.060f)
    val farCount = (82 + intensity * 115).toInt()
    val nearCount = (24 + intensity * 48).toInt()

    repeat(farCount) { index ->
        val seedX = ((index * 43 + 13) % 257) / 257f
        val seedY = ((index * 71 + 19) % 263) / 263f
        val y = ((seedY + progress * (1.05f + intensity * 0.80f)) % 1.10f) * size.height
        val x = ((seedX + progress * windX * 0.09f * gustStrength) % 1.16f) * size.width
        val length = size.height * (0.022f + intensity * 0.028f + (index % 4) * 0.003f)
        drawLine(
            color = Color(0xFFBCDDEB).copy(alpha = 0.18f + intensity * 0.20f),
            start = Offset(x, y),
            end = Offset(x + slantFar, y + length),
            strokeWidth = 1.0f + intensity * 0.55f
        )
    }

    repeat(nearCount) { index ->
        val seedX = ((index * 67 + 23) % 229) / 229f
        val seedY = ((index * 59 + 17) % 233) / 233f
        val y = ((seedY + progress * (1.38f + intensity)) % 1.12f) * size.height
        val x = ((seedX + progress * windX * 0.12f * gustStrength) % 1.18f) * size.width
        val length = size.height * (0.040f + intensity * 0.045f + (index % 3) * 0.006f)
        drawLine(
            color = Color.White.copy(alpha = 0.22f + intensity * 0.30f),
            start = Offset(x, y),
            end = Offset(x + slantNear, y + length),
            strokeWidth = 1.7f + intensity
        )
    }
}

private fun DrawScope.drawProviderHail(physics: PrecipitationPhysics, progress: Float) {
    val amount = (8 + physics.intensity * 22).toInt()
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(destination * PI / 180.0).toFloat()
    repeat(amount) { index ->
        val seedX = ((index * 73 + 5) % 157) / 157f
        val seedY = ((index * 41 + 3) % 163) / 163f
        val x = (seedX + progress * windX * 0.08f) * size.width
        val y = ((seedY + progress * 1.75f) % 1.08f) * size.height
        val radius = 1.4f + (index % 3) * 0.7f
        drawCircle(
            color = Color(0xFFEAF5FF).copy(alpha = 0.62f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawStormSplashes(physics: PrecipitationPhysics, progress: Float, flash: Float) {
    val intensity = physics.intensity.coerceIn(0f, 1f)
    val count = (15 + intensity * 30).toInt()
    repeat(count) { index ->
        val x = size.width * (((index * 31 + 9) % 107) / 107f)
        val y = size.height * (0.64f + ((index * 13) % 28) / 100f)
        val pulse = (progress * 1.45f + index * 0.123f) % 1f
        val half = size.width * (0.010f + intensity * 0.025f) * pulse
        drawOval(
            color = Color(0xFFD4EEF5).copy(alpha = ((0.20f + flash * 0.12f) * intensity * (1f - pulse)).coerceAtLeast(0f)),
            topLeft = Offset(x - half, y),
            size = Size(half * 2f, size.height * 0.0035f * (0.6f + pulse))
        )
    }
}

private fun DrawScope.drawStormExposure(intensity: Float, flash: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x2500060C).copy(alpha = 0.10f + intensity * 0.08f),
                Color.Transparent,
                Color(0xFF01080D).copy(alpha = 0.42f + intensity * 0.16f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
    if (flash > 0f) {
        drawRect(Color(0xFFDCEBFF).copy(alpha = flash * 0.075f))
    }
}

private fun mixStorm(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}
