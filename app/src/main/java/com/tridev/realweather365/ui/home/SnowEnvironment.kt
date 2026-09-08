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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SnowEnvironment(animationLevel: Int = 2, modifier: Modifier = Modifier) {
    val physics = rememberSnowPhysics()
    val intensity = physics.intensity.coerceIn(0.04f, 1f)
    val transition = rememberInfiniteTransition(label = "provider-snow-environment")

    val farSnow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (10_500 - intensity * 4_900).toInt().coerceIn(4_800, 10_500),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "provider-far-snow"
    )
    val nearSnow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (6_200 - intensity * 2_600).toInt().coerceIn(3_100, 6_200),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "provider-near-snow"
    )
    val mistShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "provider-winter-mist"
    )
    val groundDrift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "provider-ground-drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val farProgress = if (animationLevel <= 0) 0.31f else farSnow.value
        val nearProgress = if (animationLevel <= 0) 0.53f else nearSnow.value
        val mistProgress = if (animationLevel <= 0) 0.24f else mistShift.value
        val driftProgress = if (animationLevel <= 0) 0.46f else groundDrift.value
        val visibility = (physics.visibilityKm / 18.0).toFloat().coerceIn(0.16f, 1f)
        drawWinterSky(intensity, visibility)
        drawDistantSnowMountains(visibility)
        drawMidSnowRange(visibility)
        drawWinterMist(mistProgress, intensity, visibility)
        drawSnowPineForest(intensity, visibility)
        drawSnowGround(physics.accumulation, intensity)
        drawScientificSnowField(physics, farProgress, near = false)
        drawScientificSnowField(physics, nearProgress, near = true)
        if (physics.blowingSnow > 0.20f) {
            drawBlowingSnow(driftProgress, physics)
        }
        drawColdVignette(intensity, visibility)
    }
}

private fun DrawScope.drawWinterSky(intensity: Float, visibility: Float) {
    val stormTone = intensity * 0.42f
    drawRect(
        brush = Brush.verticalGradient(
            0f to mixSnow(Color(0xFF7898B2), Color(0xFF526A7C), stormTone),
            0.22f to mixSnow(Color(0xFFA2BACB), Color(0xFF7E939F), stormTone),
            0.52f to mixSnow(Color(0xFFD5E1E7), Color(0xFFB8C4C8), stormTone),
            0.76f to Color(0xFFE4EBEE).copy(alpha = 0.82f + visibility * 0.16f),
            1f to Color(0xFF8198A6)
        )
    )
}

private fun DrawScope.drawDistantSnowMountains(visibility: Float) {
    val base = size.height * 0.55f
    val mountain = Path().apply {
        moveTo(0f, base + size.height * 0.04f)
        lineTo(size.width * 0.11f, base - size.height * 0.03f)
        lineTo(size.width * 0.20f, base + size.height * 0.01f)
        lineTo(size.width * 0.34f, base - size.height * 0.17f)
        lineTo(size.width * 0.43f, base - size.height * 0.05f)
        lineTo(size.width * 0.54f, base - size.height * 0.22f)
        lineTo(size.width * 0.65f, base - size.height * 0.07f)
        lineTo(size.width * 0.78f, base - size.height * 0.18f)
        lineTo(size.width * 0.90f, base - size.height * 0.02f)
        lineTo(size.width, base - size.height * 0.10f)
        lineTo(size.width, base + size.height * 0.16f)
        lineTo(0f, base + size.height * 0.16f)
        close()
    }
    drawPath(
        path = mountain,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3F7F9).copy(alpha = 0.42f + visibility * 0.54f),
                Color(0xFFC9D6DC).copy(alpha = 0.36f + visibility * 0.48f),
                Color(0xFF8299A7).copy(alpha = 0.30f + visibility * 0.42f)
            ),
            startY = base - size.height * 0.23f,
            endY = base + size.height * 0.15f
        )
    )
}

private fun DrawScope.drawMidSnowRange(visibility: Float) {
    val top = size.height * 0.53f
    val ridge = Path().apply {
        moveTo(0f, top + size.height * 0.09f)
        lineTo(size.width * 0.14f, top + size.height * 0.015f)
        lineTo(size.width * 0.28f, top + size.height * 0.065f)
        lineTo(size.width * 0.42f, top - size.height * 0.015f)
        lineTo(size.width * 0.55f, top + size.height * 0.075f)
        lineTo(size.width * 0.70f, top + size.height * 0.005f)
        lineTo(size.width * 0.84f, top + size.height * 0.07f)
        lineTo(size.width, top + size.height * 0.015f)
        lineTo(size.width, top + size.height * 0.18f)
        lineTo(0f, top + size.height * 0.18f)
        close()
    }
    drawPath(
        ridge,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFCFD9DE).copy(alpha = 0.50f + visibility * 0.48f),
                Color(0xFF8399A4).copy(alpha = 0.46f + visibility * 0.45f),
                Color(0xFF5D7480).copy(alpha = 0.42f + visibility * 0.40f)
            ),
            startY = top - size.height * 0.02f,
            endY = top + size.height * 0.18f
        )
    )
}

private fun DrawScope.drawWinterMist(progress: Float, intensity: Float, visibility: Float) {
    val density = ((1f - visibility) * 0.68f + intensity * 0.20f).coerceIn(0.08f, 0.72f)
    repeat(6) { index ->
        val width = size.width * (0.40f + index * 0.075f)
        val rawX = size.width * (index * 0.19f) + progress * size.width * 0.66f
        val x = wrapSnow(rawX, size.width, width)
        val y = size.height * (0.48f + index * 0.035f)
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFF0F4F5).copy(alpha = density * (0.30f + index * 0.035f)),
                    Color.Transparent
                ),
                startX = x,
                endX = x + width
            ),
            topLeft = Offset(x, y),
            size = Size(width, size.height * 0.048f)
        )
    }
}

private fun DrawScope.drawSnowPineForest(intensity: Float, visibility: Float) {
    val ground = size.height * 0.72f
    repeat(24) { index ->
        val x = size.width * (index / 23f)
        val depth = when (index % 3) {
            0 -> 0.14f
            1 -> 0.10f
            else -> 0.075f
        }
        val h = size.height * depth
        val y = ground + sin(index * 1.23f).toFloat() * size.height * 0.016f
        val fade = (0.46f + visibility * 0.52f).coerceIn(0.42f, 1f)
        val pineColor = when (index % 4) {
            0 -> Color(0xFF345461)
            1 -> Color(0xFF2D4954)
            else -> Color(0xFF3B5C67)
        }.copy(alpha = fade)

        drawLine(
            color = Color(0xFF344B55).copy(alpha = fade),
            start = Offset(x, y),
            end = Offset(x, y - h * 0.92f),
            strokeWidth = 2f
        )
        repeat(4) { tier ->
            val tierY = y - h * (0.22f + tier * 0.18f)
            val half = h * (0.23f - tier * 0.035f)
            val crown = Path().apply {
                moveTo(x, tierY - h * 0.18f)
                lineTo(x - half, tierY + h * 0.12f)
                lineTo(x + half, tierY + h * 0.12f)
                close()
            }
            drawPath(crown, pineColor)
            drawLine(
                color = Color.White.copy(alpha = (0.50f + intensity * 0.35f) * fade),
                start = Offset(x - half * 0.70f, tierY + h * 0.07f),
                end = Offset(x + half * 0.18f, tierY + h * 0.01f),
                strokeWidth = 2.2f
            )
        }
    }
}

private fun DrawScope.drawSnowGround(accumulation: Float, intensity: Float) {
    val top = size.height * 0.70f
    val brightness = (0.68f + accumulation * 0.30f).coerceIn(0.68f, 0.98f)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                mixSnow(Color(0xFFCBD8DE), Color.White, brightness),
                mixSnow(Color(0xFFA9BBC5), Color(0xFFE9F0F3), accumulation * 0.58f),
                Color(0xFF8299A6)
            ),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )
    repeat(12) { index ->
        val y = top + size.height * (0.018f + index * 0.023f)
        val center = size.width * (0.48f + sin(index * 0.81f).toFloat() * 0.13f)
        val half = size.width * (0.10f + index * 0.014f)
        drawLine(
            color = Color.White.copy(alpha = 0.10f + accumulation * 0.16f + intensity * 0.06f),
            start = Offset(center - half, y),
            end = Offset(center + half, y),
            strokeWidth = 1.6f
        )
    }
}

private fun DrawScope.drawFarSnow(progress: Float, physics: SnowPhysics) {
    val count = (42 + physics.intensity * 96f).toInt().coerceIn(42, 138)
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val radians = destination / 180.0 * PI
    val drift = sin(radians).toFloat() * (0.025f + physics.windSpeedKmh.coerceIn(0, 80) / 520f)
    repeat(count) { index ->
        val phase = (index * 0.071f + progress) % 1f
        val sway = sin(progress * 6.28318f + index * 0.83f).toFloat() * size.width * 0.012f
        val xBase = ((index * 0.173f + 0.07f) % 1f) * size.width
        val x = wrapSnowPoint(xBase + sway + phase * size.width * drift, size.width)
        val y = phase * size.height
        val radius = 0.9f + (index % 4) * 0.48f + physics.intensity * 0.65f
        drawCircle(
            color = Color.White.copy(alpha = 0.34f + (index % 4) * 0.07f + physics.intensity * 0.12f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawNearSnow(progress: Float, physics: SnowPhysics) {
    val count = (16 + physics.intensity * 54f).toInt().coerceIn(16, 70)
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val radians = destination / 180.0 * PI
    val drift = sin(radians).toFloat() * (0.045f + physics.windGustKmh.coerceIn(0, 100) / 430f)
    repeat(count) { index ->
        val phase = (index * 0.137f + progress * 1.08f) % 1f
        val flutter = sin(progress * 8.1f + index * 0.77f).toFloat() * size.width * 0.022f
        val xBase = ((index * 0.237f + 0.11f) % 1f) * size.width
        val x = wrapSnowPoint(xBase + flutter + phase * size.width * drift, size.width)
        val y = phase * size.height
        val radius = 2.1f + (index % 4) * 0.85f + physics.intensity * 1.4f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.93f), Color.White.copy(alpha = 0.06f)),
                center = Offset(x, y),
                radius = radius * 2.1f
            ),
            radius = radius * 2.1f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.76f + physics.intensity * 0.18f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawBlowingSnow(progress: Float, physics: SnowPhysics) {
    val count = (18 + physics.blowingSnow * 54f).toInt().coerceIn(18, 72)
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val radians = destination / 180.0 * PI
    val flowX = sin(radians).toFloat()
    val flowY = (-cos(radians)).toFloat()
    repeat(count) { index ->
        val depth = (index % 5) / 4f
        val yBase = size.height * (0.70f + depth * 0.25f)
        val travel = progress * size.width * (0.65f + physics.blowingSnow * 1.25f)
        val xBase = ((index * 0.191f + 0.03f) % 1f) * size.width
        val x = wrapSnowPoint(xBase + travel * flowX, size.width)
        val y = yBase + progress * size.height * 0.055f * flowY + sin(index * 1.7f + progress * 10f).toFloat() * 8f
        val length = 5f + physics.blowingSnow * 12f + (index % 3) * 2f
        drawLine(
            color = Color.White.copy(alpha = 0.18f + physics.blowingSnow * 0.40f),
            start = Offset(x, y),
            end = Offset(x - flowX * length, y - flowY * length * 0.35f),
            strokeWidth = 1.1f + (index % 2) * 0.5f
        )
    }
}

private fun DrawScope.drawColdVignette(intensity: Float, visibility: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x1607192A),
                Color.Transparent,
                Color(0x65041722).copy(alpha = 0.20f + intensity * 0.18f + (1f - visibility) * 0.10f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
}

private fun mixSnow(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}

private fun wrapSnow(value: Float, width: Float, itemWidth: Float): Float {
    val span = width + itemWidth * 2f
    var normalized = (value + itemWidth) % span
    if (normalized < 0f) normalized += span
    return normalized - itemWidth
}

private fun wrapSnowPoint(value: Float, width: Float): Float {
    var normalized = value % width
    if (normalized < 0f) normalized += width
    return normalized
}
