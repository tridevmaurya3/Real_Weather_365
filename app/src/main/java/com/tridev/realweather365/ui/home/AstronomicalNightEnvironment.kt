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
fun AstronomicalNightEnvironment(
    moon: MoonVisualState,
    solar: SolarVisualState,
    cloudCover: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "astronomical-night")
    val waterShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moon-water-shimmer"
    )
    val starPulse = transition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "astronomical-star-pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cloudFraction = cloudCover.coerceIn(0, 100) / 100f
        val moonlight = if (moon.aboveHorizon) {
            (moon.illuminationFraction.toFloat() * (1f - cloudFraction * 0.78f)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val starVisibility = (
            solar.starVisibility *
                (1f - cloudFraction * 0.92f) *
                (1f - moonlight * 0.48f)
            ).coerceIn(0f, 1f)

        drawNightSkyAstronomy(moonlight, solar.twilightStrength)
        drawAstronomicalStars(starVisibility, starPulse.value)
        drawRealMoon(moon, cloudFraction)
        drawNightMountainWorld(moonlight)
        drawNightLake(moon, moonlight, waterShift.value)
        drawNightForestAstronomy(moonlight)
        // RealCloudSystem owns night clouds as well, preventing a second
        // cartoon-like layer from drifting through the volumetric deck.
        drawNightVignetteAstronomy(moonlight)
    }
}

private fun DrawScope.drawNightSkyAstronomy(moonlight: Float, twilight: Float) {
    val moonTop = mixNight(Color(0xFF050D20), Color(0xFF102A4B), moonlight * 0.75f)
    val moonMid = mixNight(Color(0xFF0A1C35), Color(0xFF214D72), moonlight * 0.72f)
    val moonHorizon = mixNight(Color(0xFF102D45), Color(0xFF35647D), moonlight * 0.58f)
    val twilightTint = Color(0xFF3B315E)

    drawRect(
        brush = Brush.verticalGradient(
            0f to mixNight(moonTop, twilightTint, twilight * 0.18f),
            0.36f to mixNight(moonMid, Color(0xFF59405A), twilight * 0.12f),
            0.62f to moonHorizon,
            1f to Color(0xFF031019)
        )
    )
}

private fun DrawScope.drawAstronomicalStars(visibility: Float, pulse: Float) {
    if (visibility <= 0.01f) return
    repeat(76) { index ->
        val x = ((index * 0.173f + 0.031f) % 1f) * size.width
        val y = ((index * 0.097f + 0.021f) % 0.48f) * size.height
        val twinkle = if (index % 3 == 0) pulse else (0.70f + pulse * 0.24f)
        val alpha = (visibility * twinkle * (0.34f + (index % 5) * 0.10f)).coerceAtMost(0.92f)
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 0.65f + (index % 4) * 0.34f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawRealMoon(moon: MoonVisualState, cloudFraction: Float) {
    if (!moon.aboveHorizon || moon.altitudeDegrees < -1.2) return

    val attenuation = (1f - cloudFraction * 0.82f).coerceIn(0.12f, 1f)
    val center = Offset(size.width * moon.visualX, size.height * moon.visualY)
    val radius = size.width * 0.060f
    val illumination = moon.illuminationFraction.toFloat().coerceIn(0f, 1f)
    val glowStrength = (0.08f + illumination * 0.34f) * attenuation
    val glowRadius = radius * (2.5f + illumination * 1.6f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFD9EEFF).copy(alpha = glowStrength),
                Color(0xFF9FCFF2).copy(alpha = glowStrength * 0.34f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    val dark = Color(0xFF0A1321).copy(alpha = 0.96f)
    val lit = Color(0xFFEAF3F7).copy(alpha = attenuation)
    drawCircle(dark, radius, center)
    drawCircle(lit, radius, center)

    val phase = moon.phaseFraction.coerceIn(0.0, 1.0)
    val shadowOffset = if (phase <= 0.5) {
        (-2.05 * radius * (phase / 0.5)).toFloat()
    } else {
        (2.05 * radius * ((1.0 - phase) / 0.5)).toFloat()
    }
    drawCircle(
        color = dark,
        radius = radius * 1.01f,
        center = Offset(center.x + shadowOffset, center.y)
    )

    if (illumination > 0.10f) {
        val craterAlpha = (0.10f + illumination * 0.12f) * attenuation
        drawCircle(Color(0xFF9CAAB5).copy(alpha = craterAlpha), radius * 0.15f, Offset(center.x - radius * 0.22f, center.y - radius * 0.12f))
        drawCircle(Color(0xFFA6B4BE).copy(alpha = craterAlpha * 0.86f), radius * 0.10f, Offset(center.x + radius * 0.24f, center.y + radius * 0.16f))
        drawCircle(Color(0xFF8F9DA8).copy(alpha = craterAlpha * 0.68f), radius * 0.07f, Offset(center.x + radius * 0.08f, center.y - radius * 0.30f))
    }
}

private fun DrawScope.drawAstronomicalNightClouds(cloudCover: Int, progress: Float) {
    val cover = cloudCover.coerceIn(0, 100)
    if (cover < 6) return
    val count = (1 + cover / 14).coerceIn(1, 8)
    val alphaBase = (0.11f + cover / 100f * 0.34f).coerceAtMost(0.43f)

    repeat(count) { index ->
        val width = size.width * (0.20f + (index % 3) * 0.07f)
        val height = size.height * (0.024f + (index % 2) * 0.010f)
        val x = (((progress + index * 0.19f) % 1.35f) * size.width) - width * 0.55f
        val y = size.height * (0.12f + (index % 6) * 0.065f)
        val color = Color(0xFFB7C7D2).copy(alpha = alphaBase * (0.72f + (index % 3) * 0.10f))
        drawOval(color, Offset(x, y), Size(width, height))
        drawCircle(color, width * 0.14f, Offset(x + width * 0.28f, y + height * 0.10f))
        drawCircle(color, width * 0.18f, Offset(x + width * 0.52f, y - height * 0.07f))
        drawCircle(color, width * 0.13f, Offset(x + width * 0.72f, y + height * 0.08f))
    }
}

private fun DrawScope.drawNightMountainWorld(moonlight: Float) {
    val horizon = size.height * 0.52f
    val distant = Path().apply {
        moveTo(0f, horizon + size.height * 0.05f)
        lineTo(size.width * 0.12f, horizon - size.height * 0.02f)
        lineTo(size.width * 0.26f, horizon + size.height * 0.02f)
        lineTo(size.width * 0.40f, horizon - size.height * 0.085f)
        lineTo(size.width * 0.53f, horizon + size.height * 0.005f)
        lineTo(size.width * 0.66f, horizon - size.height * 0.13f)
        lineTo(size.width * 0.80f, horizon - size.height * 0.025f)
        lineTo(size.width, horizon + size.height * 0.02f)
        lineTo(size.width, horizon + size.height * 0.18f)
        lineTo(0f, horizon + size.height * 0.18f)
        close()
    }
    drawPath(
        distant,
        brush = Brush.verticalGradient(
            listOf(
                mixNight(Color(0xFF273544), Color(0xFF748697), moonlight * 0.55f),
                mixNight(Color(0xFF152738), Color(0xFF3D586B), moonlight * 0.48f),
                Color(0xFF0B1E2B)
            ),
            startY = horizon - size.height * 0.14f,
            endY = horizon + size.height * 0.17f
        )
    )

    val near = Path().apply {
        moveTo(0f, horizon + size.height * 0.13f)
        lineTo(size.width * 0.18f, horizon + size.height * 0.07f)
        lineTo(size.width * 0.36f, horizon + size.height * 0.12f)
        lineTo(size.width * 0.56f, horizon + size.height * 0.07f)
        lineTo(size.width * 0.76f, horizon + size.height * 0.13f)
        lineTo(size.width, horizon + size.height * 0.075f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(near, mixNight(Color(0xFF061714), Color(0xFF0D2B27), moonlight * 0.42f))
}

private fun DrawScope.drawNightLake(moon: MoonVisualState, moonlight: Float, progress: Float) {
    val top = size.height * 0.61f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                mixNight(Color(0xFF102A3A), Color(0xFF2B5B75), moonlight * 0.62f),
                mixNight(Color(0xFF071D2A), Color(0xFF19445B), moonlight * 0.58f),
                Color(0xFF03131D)
            ),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )

    if (moon.aboveHorizon && moon.altitudeDegrees > 2.0 && moonlight > 0.03f) {
        val x = size.width * moon.visualX
        repeat(18) { index ->
            val phase = progress * 6.28318f + index * 0.73f
            val y = top + size.height * (0.012f + index * 0.0105f)
            val half = size.width * (0.014f + index * 0.0035f)
            val drift = sin(phase) * size.width * 0.016f
            drawLine(
                color = Color(0xFFD7EEFF).copy(alpha = (moonlight * (0.22f - index * 0.008f)).coerceAtLeast(0.01f)),
                start = Offset(x - half + drift, y),
                end = Offset(x + half + drift, y),
                strokeWidth = 1.0f + (index % 3) * 0.35f
            )
        }
    }
}

private fun DrawScope.drawNightForestAstronomy(moonlight: Float) {
    val baseY = size.height * 0.67f
    val tree = mixNight(Color(0xFF03100D), Color(0xFF102B24), moonlight * 0.38f)
    repeat(28) { index ->
        val x = size.width * index / 27f
        val h = size.height * (0.026f + (index % 5) * 0.006f)
        val path = Path().apply {
            moveTo(x, baseY - h)
            lineTo(x - h * 0.18f, baseY)
            lineTo(x + h * 0.18f, baseY)
            close()
        }
        drawPath(path, tree.copy(alpha = 0.95f))
    }
}

private fun DrawScope.drawNightVignetteAstronomy(moonlight: Float) {
    val darkness = (0.78f - moonlight * 0.24f).coerceIn(0.48f, 0.82f)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color(0x22030B10), Color(0xFF01080D).copy(alpha = darkness)),
            startY = size.height * 0.66f,
            endY = size.height
        )
    )
}

private fun mixNight(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}
