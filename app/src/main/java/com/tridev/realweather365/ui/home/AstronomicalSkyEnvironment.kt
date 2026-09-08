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
import kotlin.math.sin

@Composable
fun AstronomicalSkyEnvironment(
    solar: SolarVisualState,
    cloudCover: Int,
    humidity: Int = 50,
    visibilityKm: Double? = null,
    directRadiation: Double = 0.0,
    diffuseRadiation: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val optics = AtmosphericOpticsEngine.calculate(solar.elevationDegrees, humidity, visibilityKm, cloudCover, directRadiation, diffuseRadiation)
    val transition = rememberInfiniteTransition(label = "astronomical-sky")
    val waterShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "solar-reflection"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawAstronomicalSky(solar, optics)
        drawTwilightStars(solar)
        drawAstronomicalSun(solar, cloudCover, optics)
        // Clouds are rendered once by RealCloudSystem so their live coverage,
        // wind, depth and lighting stay physically consistent across scenes.
        drawSolarMountainWorld(solar)
        drawSolarLake(solar, waterShift.value)
        drawSolarForest(solar)
        drawSolarForegroundVignette(solar)
    }
}

@Composable
fun SolarLightingOverlay(
    solar: SolarVisualState,
    cloudCover: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudSuppression = (1f - cloudCover.coerceIn(0, 100) / 135f).coerceIn(0.25f, 1f)
        if (solar.twilightStrength > 0.02f) {
            val warm = solar.warmth * cloudSuppression
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF3A315C).copy(alpha = 0.08f * solar.twilightStrength),
                    0.42f to Color(0xFFFF8E55).copy(alpha = 0.13f * warm),
                    0.62f to Color(0xFFFFC66B).copy(alpha = 0.09f * warm),
                    1f to Color.Transparent
                )
            )
        }
        if (solar.elevationDegrees < -4.0) {
            val night = ((-solar.elevationDegrees - 4.0) / 18.0).coerceIn(0.0, 1.0).toFloat()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF001027).copy(alpha = 0.18f * night),
                    0.6f to Color(0xFF031522).copy(alpha = 0.10f * night),
                    1f to Color.Transparent
                )
            )
        }
    }
}

private fun DrawScope.drawAstronomicalSky(solar: SolarVisualState, optics: AtmosphericOptics) {
    val t = solar.daylight
    val topNight = Color(0xFF06132A)
    val topDay = mix(Color(0xFF237FB8), Color(0xFF087FD5), optics.rayleigh)
    val midNight = Color(0xFF173854)
    val midDay = Color(0xFF63BAE5)
    val horizonNight = Color(0xFF334B62)
    val horizonDay = mix(Color(0xFFD5EEF5), Color(0xFFE7D0B2), optics.mie * optics.horizonExtinction * 0.38f)

    val warmAmount = solar.warmth
    val horizonWarm = if (solar.isRising) Color(0xFFFFB067) else Color(0xFFFF875E)

    drawRect(
        brush = Brush.verticalGradient(
            0f to mix(topNight, topDay, t),
            0.34f to mix(midNight, midDay, t),
            0.58f to mix(mix(horizonNight, horizonDay, t), horizonWarm, maxOf(warmAmount * 0.72f, optics.sunsetReddening * 0.52f)),
            0.73f to mix(Color(0xFF2A4853), Color(0xFFB8DADE), t * 0.72f),
            1f to Color(0xFF17333D)
        )
    )

    if (solar.twilightStrength > 0.02f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    horizonWarm.copy(alpha = 0.30f * solar.twilightStrength),
                    Color(0xFFFFC77F).copy(alpha = 0.12f * solar.twilightStrength),
                    Color.Transparent
                ),
                center = Offset(size.width * solar.visualX, size.height * 0.48f),
                radius = size.width * 0.62f
            )
        )
    }
}

private fun DrawScope.drawTwilightStars(solar: SolarVisualState) {
    if (solar.starVisibility <= 0.02f) return
    repeat(48) { index ->
        val x = ((index * 0.171f + 0.041f) % 1f) * size.width
        val y = ((index * 0.087f + 0.023f) % 0.40f) * size.height
        val alpha = solar.starVisibility * (0.32f + (index % 5) * 0.11f)
        drawCircle(
            color = Color.White.copy(alpha = alpha.coerceAtMost(0.82f)),
            radius = 0.65f + (index % 3) * 0.40f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawAstronomicalSun(solar: SolarVisualState, cloudCover: Int, optics: AtmosphericOptics) {
    if (!solar.sunVisible) return
    val attenuation = ((1f - cloudCover.coerceIn(0, 100) / 125f) * optics.directExposure).coerceIn(0.08f, 1f)
    val center = Offset(size.width * solar.visualX, size.height * solar.visualY)
    val lowSun = (1f - (solar.elevationDegrees / 22.0).coerceIn(0.0, 1.0)).toFloat()
    val glowRadius = size.width * (0.18f + 0.12f * lowSun)
    val coreRadius = size.width * (0.028f + 0.008f * lowSun)
    val warm = mix(if (solar.isRising) Color(0xFFFFD37C) else Color(0xFFFFAA62), Color(0xFFFF704A), optics.sunsetReddening * 0.52f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.86f * attenuation),
                warm.copy(alpha = 0.58f * attenuation),
                Color(0xFFFF8C4F).copy(alpha = 0.18f * attenuation * lowSun),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    drawCircle(
        color = mix(Color(0xFFFFF6C7), warm, lowSun * 0.44f).copy(alpha = attenuation),
        radius = coreRadius,
        center = center
    )

    if (lowSun > 0.18f) {
        drawOval(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, warm.copy(alpha = 0.20f * attenuation * lowSun), Color.Transparent)
            ),
            topLeft = Offset(0f, center.y - size.height * 0.010f),
            size = Size(size.width, size.height * 0.020f)
        )
    }
}

private fun DrawScope.drawSolarClouds(solar: SolarVisualState, cloudCover: Int, progress: Float) {
    val cover = cloudCover.coerceIn(0, 100)
    if (cover < 8) return
    val count = (2 + cover / 16).coerceIn(2, 8)
    val warmTint = if (solar.isRising) Color(0xFFFFD4B2) else Color(0xFFFFB6A0)
    val base = mix(Color(0xFFEAF3F5), warmTint, solar.warmth * 0.48f)
    val alphaBase = (0.13f + cover / 100f * 0.25f).coerceAtMost(0.38f)

    repeat(count) { index ->
        val drift = ((progress + index * 0.17f) % 1.25f) * size.width
        val width = size.width * (0.18f + (index % 3) * 0.055f)
        val height = size.height * (0.022f + (index % 2) * 0.010f)
        val x = drift - width * 0.35f
        val y = size.height * (0.13f + (index % 5) * 0.066f)
        val c = base.copy(alpha = alphaBase * (0.78f + (index % 3) * 0.08f))
        drawOval(c, Offset(x, y), Size(width, height))
        drawCircle(c, width * 0.14f, Offset(x + width * 0.28f, y + height * 0.10f))
        drawCircle(c, width * 0.18f, Offset(x + width * 0.52f, y - height * 0.08f))
        drawCircle(c, width * 0.13f, Offset(x + width * 0.72f, y + height * 0.08f))
    }
}

private fun DrawScope.drawSolarMountainWorld(solar: SolarVisualState) {
    val horizon = size.height * 0.51f
    val daylight = solar.daylight
    val warm = solar.warmth

    val distant = Path().apply {
        moveTo(0f, horizon + size.height * 0.05f)
        lineTo(size.width * 0.12f, horizon - size.height * 0.03f)
        lineTo(size.width * 0.25f, horizon + size.height * 0.01f)
        lineTo(size.width * 0.38f, horizon - size.height * 0.10f)
        lineTo(size.width * 0.50f, horizon - size.height * 0.015f)
        lineTo(size.width * 0.63f, horizon - size.height * 0.15f)
        lineTo(size.width * 0.76f, horizon - size.height * 0.035f)
        lineTo(size.width * 0.88f, horizon - size.height * 0.09f)
        lineTo(size.width, horizon + size.height * 0.02f)
        lineTo(size.width, horizon + size.height * 0.17f)
        lineTo(0f, horizon + size.height * 0.17f)
        close()
    }
    drawPath(
        distant,
        brush = Brush.verticalGradient(
            listOf(
                mix(Color(0xFF53636C), Color(0xFFE8F1F3), daylight * 0.82f),
                mix(Color(0xFF263D45), Color(0xFF6D8E99), daylight * 0.72f),
                mix(Color(0xFF1A3037), Color(0xFF41636D), daylight * 0.65f)
            ),
            startY = horizon - size.height * 0.17f,
            endY = horizon + size.height * 0.16f
        )
    )

    val snow = mix(Color(0xFFB4C3CA), Color.White, daylight).copy(alpha = 0.88f)
    val snowPeak = Path().apply {
        moveTo(size.width * 0.52f, horizon - size.height * 0.055f)
        lineTo(size.width * 0.63f, horizon - size.height * 0.15f)
        lineTo(size.width * 0.72f, horizon - size.height * 0.052f)
        lineTo(size.width * 0.67f, horizon - size.height * 0.081f)
        lineTo(size.width * 0.63f, horizon - size.height * 0.058f)
        lineTo(size.width * 0.59f, horizon - size.height * 0.086f)
        close()
    }
    drawPath(snowPeak, mix(snow, Color(0xFFFFC49A), warm * 0.24f))

    val middle = Path().apply {
        moveTo(0f, horizon + size.height * 0.095f)
        lineTo(size.width * 0.18f, horizon + size.height * 0.025f)
        lineTo(size.width * 0.35f, horizon + size.height * 0.08f)
        lineTo(size.width * 0.52f, horizon + size.height * 0.018f)
        lineTo(size.width * 0.70f, horizon + size.height * 0.086f)
        lineTo(size.width * 0.86f, horizon + size.height * 0.02f)
        lineTo(size.width, horizon + size.height * 0.08f)
        lineTo(size.width, horizon + size.height * 0.19f)
        lineTo(0f, horizon + size.height * 0.19f)
        close()
    }
    drawPath(middle, mix(Color(0xFF102A29), Color(0xFF255248), daylight * 0.62f))

    val near = Path().apply {
        moveTo(0f, horizon + size.height * 0.13f)
        lineTo(size.width * 0.16f, horizon + size.height * 0.075f)
        lineTo(size.width * 0.34f, horizon + size.height * 0.12f)
        lineTo(size.width * 0.55f, horizon + size.height * 0.075f)
        lineTo(size.width * 0.74f, horizon + size.height * 0.13f)
        lineTo(size.width, horizon + size.height * 0.072f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(near, mix(Color(0xFF071B18), Color(0xFF0D352B), daylight * 0.58f))
}

private fun DrawScope.drawSolarLake(solar: SolarVisualState, progress: Float) {
    val top = size.height * 0.60f
    val daylight = solar.daylight
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                mix(Color(0xFF173D52), Color(0xFF53A8BF), daylight * 0.78f),
                mix(Color(0xFF0A2D3E), Color(0xFF14708F), daylight * 0.82f),
                mix(Color(0xFF051B27), Color(0xFF073F56), daylight * 0.72f)
            ),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )

    if (solar.sunVisible && solar.elevationDegrees < 24.0) {
        val x = size.width * solar.visualX
        val warm = if (solar.isRising) Color(0xFFFFD084) else Color(0xFFFFA46E)
        repeat(15) { index ->
            val phase = progress * 6.28318f + index * 0.71f
            val y = top + size.height * (0.012f + index * 0.0105f)
            val half = size.width * (0.018f + index * 0.0038f)
            val drift = sin(phase) * size.width * 0.018f
            drawLine(
                color = warm.copy(alpha = (0.16f - index * 0.006f).coerceAtLeast(0.025f)),
                start = Offset(x - half + drift, y),
                end = Offset(x + half + drift, y),
                strokeWidth = 1.2f + index % 3 * 0.35f
            )
        }
    }
}

private fun DrawScope.drawSolarForest(solar: SolarVisualState) {
    val baseY = size.height * 0.665f
    val tree = mix(Color(0xFF081D19), Color(0xFF164536), solar.daylight * 0.62f)
    repeat(27) { index ->
        val x = size.width * index / 26f
        val h = size.height * (0.025f + (index % 5) * 0.006f)
        val p = Path().apply {
            moveTo(x, baseY - h)
            lineTo(x - h * 0.18f, baseY)
            lineTo(x + h * 0.18f, baseY)
            close()
        }
        drawPath(p, tree.copy(alpha = 0.92f))
    }
}

private fun DrawScope.drawSolarForegroundVignette(solar: SolarVisualState) {
    val darkness = (1f - solar.daylight * 0.65f).coerceIn(0.25f, 0.90f)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xFF04100F).copy(alpha = 0.22f), Color(0xFF020A09).copy(alpha = darkness)),
            startY = size.height * 0.68f,
            endY = size.height
        )
    )
}

private fun mix(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}
