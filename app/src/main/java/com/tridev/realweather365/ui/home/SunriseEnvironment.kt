package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
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
fun SunriseEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sunrise-environment")
    val mistShift = transition.animateFloat(
        initialValue = -0.16f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mist-shift"
    )
    val glowPulse = transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun-glow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSunriseSky()
        drawSunriseSun(glowPulse.value)
        drawHighClouds(mistShift.value)
        drawDistantRanges()
        drawValleyMist(mistShift.value)
        drawNearRidge()
        drawWarmForeground()
    }
}

private fun DrawScope.drawSunriseSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF183253),
            0.22f to Color(0xFF4B5B72),
            0.48f to Color(0xFFD88A67),
            0.68f to Color(0xFFF5B36E),
            0.82f to Color(0xFFF5D4A1),
            1f to Color(0xFF6B756D)
        )
    )
}

private fun DrawScope.drawSunriseSun(pulse: Float) {
    val center = Offset(size.width * 0.50f, size.height * 0.405f)
    val glowRadius = size.width * 0.24f * pulse

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9D8).copy(alpha = 0.98f),
                Color(0xFFFFC766).copy(alpha = 0.58f),
                Color(0xFFFF9E55).copy(alpha = 0.20f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    drawCircle(
        color = Color(0xFFFFE6A1),
        radius = size.width * 0.032f,
        center = center
    )

    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0xFFFFE2A6).copy(alpha = 0.24f), Color.Transparent)
        ),
        topLeft = Offset(0f, center.y - size.height * 0.012f),
        size = Size(size.width, size.height * 0.024f)
    )
}

private fun DrawScope.drawHighClouds(progress: Float) {
    val x = progress * size.width - size.width * 0.32f
    val y = size.height * 0.19f
    val cloud = Color(0xFFFFD9BF).copy(alpha = 0.18f)

    drawOval(
        color = cloud,
        topLeft = Offset(x, y),
        size = Size(size.width * 0.32f, size.height * 0.034f)
    )
    drawOval(
        color = cloud.copy(alpha = 0.12f),
        topLeft = Offset(size.width - x - size.width * 0.10f, y + size.height * 0.075f),
        size = Size(size.width * 0.24f, size.height * 0.026f)
    )
}

private fun DrawScope.drawDistantRanges() {
    val base = size.height * 0.53f

    val backRange = Path().apply {
        moveTo(0f, base + size.height * 0.055f)
        lineTo(size.width * 0.11f, base - size.height * 0.012f)
        lineTo(size.width * 0.24f, base + size.height * 0.018f)
        lineTo(size.width * 0.38f, base - size.height * 0.065f)
        lineTo(size.width * 0.52f, base + size.height * 0.006f)
        lineTo(size.width * 0.68f, base - size.height * 0.078f)
        lineTo(size.width * 0.83f, base + size.height * 0.020f)
        lineTo(size.width, base - size.height * 0.020f)
        lineTo(size.width, base + size.height * 0.14f)
        lineTo(0f, base + size.height * 0.14f)
        close()
    }
    drawPath(
        backRange,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF6D6764), Color(0xFF4E5858)),
            startY = base - size.height * 0.10f,
            endY = base + size.height * 0.14f
        )
    )

    val middleRange = Path().apply {
        moveTo(0f, base + size.height * 0.09f)
        lineTo(size.width * 0.18f, base + size.height * 0.02f)
        lineTo(size.width * 0.34f, base + size.height * 0.075f)
        lineTo(size.width * 0.49f, base + size.height * 0.025f)
        lineTo(size.width * 0.64f, base + size.height * 0.09f)
        lineTo(size.width * 0.81f, base + size.height * 0.025f)
        lineTo(size.width, base + size.height * 0.085f)
        lineTo(size.width, base + size.height * 0.18f)
        lineTo(0f, base + size.height * 0.18f)
        close()
    }
    drawPath(middleRange, Color(0xFF34453F).copy(alpha = 0.96f))
}

private fun DrawScope.drawValleyMist(progress: Float) {
    val valleyY = size.height * 0.565f

    repeat(5) { layer ->
        val shift = ((progress + layer * 0.21f) % 1.35f) * size.width
        val alpha = 0.15f - layer * 0.018f
        val width = size.width * (0.42f + layer * 0.06f)
        val height = size.height * (0.034f + layer * 0.005f)
        val x = shift - width * 0.55f
        val y = valleyY + layer * size.height * 0.018f

        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFE4C0).copy(alpha = alpha),
                    Color.White.copy(alpha = alpha * 0.58f),
                    Color.Transparent
                ),
                startX = x,
                endX = x + width
            ),
            topLeft = Offset(x, y),
            size = Size(width, height)
        )
    }
}

private fun DrawScope.drawNearRidge() {
    val top = size.height * 0.62f
    val ridge = Path().apply {
        moveTo(0f, top + size.height * 0.035f)
        lineTo(size.width * 0.10f, top - size.height * 0.020f)
        lineTo(size.width * 0.19f, top + size.height * 0.008f)
        lineTo(size.width * 0.29f, top - size.height * 0.040f)
        lineTo(size.width * 0.42f, top + size.height * 0.018f)
        lineTo(size.width * 0.55f, top - size.height * 0.028f)
        lineTo(size.width * 0.68f, top + size.height * 0.020f)
        lineTo(size.width * 0.79f, top - size.height * 0.036f)
        lineTo(size.width * 0.91f, top + size.height * 0.006f)
        lineTo(size.width, top - size.height * 0.012f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

    drawPath(
        ridge,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF233C31), Color(0xFF102A23), Color(0xFF071A17)),
            startY = top - size.height * 0.05f,
            endY = size.height
        )
    )
}

private fun DrawScope.drawWarmForeground() {
    val baseY = size.height * 0.78f

    repeat(26) { index ->
        val x = size.width * (index / 25f)
        val wave = sin(index * 1.7f) * size.height * 0.012f
        val h = size.height * (0.045f + (index % 5) * 0.009f)
        val trunk = Color(0xFF15251F)
        val leaf = if (index % 3 == 0) Color(0xFF1A3A2D) else Color(0xFF153126)

        drawLine(
            color = trunk,
            start = Offset(x, baseY + wave),
            end = Offset(x, baseY + wave - h * 0.45f),
            strokeWidth = 1.6f
        )
        drawCircle(
            color = leaf,
            radius = h * 0.12f,
            center = Offset(x, baseY + wave - h * 0.54f)
        )
        drawCircle(
            color = leaf.copy(alpha = 0.96f),
            radius = h * 0.10f,
            center = Offset(x - h * 0.06f, baseY + wave - h * 0.46f)
        )
        drawCircle(
            color = leaf.copy(alpha = 0.92f),
            radius = h * 0.095f,
            center = Offset(x + h * 0.07f, baseY + wave - h * 0.45f)
        )
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xAA061411), Color(0xE8030D0B)),
            startY = size.height * 0.70f,
            endY = size.height
        )
    )
}
